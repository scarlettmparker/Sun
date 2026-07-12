package com.sun.graphql.audit.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.base.audit.context.AuditContext;
import com.sun.base.audit.context.AuditRequestSnapshot;
import com.sun.base.audit.context.OperationMetadata;
import com.sun.base.audit.enums.AuditOutcome;
import com.sun.base.audit.enums.OperationType;
import com.sun.graphql.audit.graphql.AuditOperationRegistry;
import com.sun.graphql.audit.service.AuditEventService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Captures each request and writes an audit row for it via AuditEventService.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuditFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(AuditFilter.class);

  private static final String GRAPHQL_PATH = "/graphql";

  /** Cap on how many request body bytes we cache/audit, to bound memory use. */
  private static final int MAX_CACHED_BODY_BYTES = 64 * 1024;

  private final AuditContext auditContext;
  private final AuditEventService auditEventService;
  private final AuditOperationRegistry registry;
  private final ObjectMapper objectMapper;

  public AuditFilter(AuditContext auditContext,
                     AuditEventService auditEventService,
                     AuditOperationRegistry registry,
                     ObjectMapper objectMapper) {
    this.auditContext = auditContext;
    this.auditEventService = auditEventService;
    this.registry = registry;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Skip CORS preflight; everything else (including failures) is audited.
    return "OPTIONS".equalsIgnoreCase(request.getMethod());
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    auditContext.begin();
    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_CACHED_BODY_BYTES);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    auditContext.setStartedAt(Instant.now());
    auditContext.setEndpoint(request.getRequestURI());
    auditContext.setMethod(request.getMethod());
    auditContext.setIpAddress(clientIp(request));
    auditContext.setUserAgent(forwardedHeader(request, "User-Agent"));
    Object correlation = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
    if (correlation instanceof String cid) {
      try {
        auditContext.setCorrelationId(UUID.fromString(cid));
      } catch (IllegalArgumentException ignored) {
        auditContext.setCorrelationId(UUID.randomUUID());
      }
    }

    try {
      chain.doFilter(wrappedRequest, wrappedResponse);
    } finally {
      try {
        auditContext.setHttpStatus(wrappedResponse.getStatus());
        ensureOperationRecorded(wrappedRequest);
        long durationMs = auditContext.getStartedAt() == null
            ? 0L
            : Duration.between(auditContext.getStartedAt(), Instant.now()).toMillis();
        // Copy thread-local state into a snapshot for the async writer (separate thread).
        AuditRequestSnapshot snapshot = new AuditRequestSnapshot(
            List.copyOf(auditContext.operations()),
            auditContext.getCorrelationId(),
            auditContext.getUserId(),
            auditContext.getEndpoint(),
            auditContext.getIpAddress(),
            auditContext.getUserAgent(),
            auditContext.getHttpStatus());
        auditEventService.persist(snapshot, durationMs);
      } catch (Exception e) {
        // Auditing must not break the response.
        logger.warn("Failed to persist audit event (correlationId={})",
            auditContext.getCorrelationId(), e);
      } finally {
        auditContext.clear();
        wrappedResponse.copyBodyToResponse();
      }
    }
  }

  /**
   * Guarantees the context has at least one operation to audit. REST endpoints
   * get a synthetic operation built from the cached body; GraphQL requests that
   * never reached the instrumentation (parse error, unauthorised) get a fallback
   * so the attempt is still recorded.
   *
   * @param request the caching wrapper around the original request
   */
  private void ensureOperationRecorded(ContentCachingRequestWrapper request) {
    boolean isGraphql = GRAPHQL_PATH.equalsIgnoreCase(request.getRequestURI());

    if (!isGraphql) {
      // REST endpoints are not seen by the GraphQL instrumentation, so build
      // a synthetic operation from the cached request body here.
      OperationMetadata meta = registry.forEndpoint(request.getMethod(), request.getRequestURI());
      Object variables = parseBody(request.getContentAsByteArray());
      AuditOutcome outcome = outcomeFromStatus(auditContext.getHttpStatus());
      auditContext.addOperation(new AuditContext.AuditOperation(
          request.getRequestURI(), meta, variables, null, outcome, null));
      return;
    }

    if (auditContext.operations().isEmpty()) {
      // GraphQL request that never reached/passed the instrumentation
      // (parse error, unauthorised). Record a fallback so the attempt is
      // still captured.
      AuditOutcome outcome = outcomeFromStatus(auditContext.getHttpStatus());
      OperationMetadata meta = OperationMetadata.unknown(OperationType.MUTATION, null);
      auditContext.addOperation(new AuditContext.AuditOperation(
          "unknown", meta, parseBody(request.getContentAsByteArray()), null, outcome, null));
    }
  }

  /**
   * Maps an HTTP status to an audit outcome.
   *
   * @param status the response status code
   * @return SUCCESS for 2xx, UNAUTHORIZED for 401/403, otherwise FAILURE
   */
  private AuditOutcome outcomeFromStatus(int status) {
    if (status >= 200 && status < 300) {
      return AuditOutcome.SUCCESS;
    }
    if (status == 401 || status == 403) {
      return AuditOutcome.UNAUTHORIZED;
    }
    return AuditOutcome.FAILURE;
  }

  /**
   * Parses the cached request body into a map, falling back to a raw
   * body wrapper when the content is not valid JSON.
   *
   * @param body the cached request bytes
   * @return the parsed body, or null when empty
   */
  private Object parseBody(byte[] body) {
    if (body == null || body.length == 0) {
      return null;
    }
    try {
      return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      return Map.of("body", new String(body));
    }
  }

  /**
   * Resolves a header, preferring the X-Forwarded- variant set by an upstream
   * proxy over the direct value, which for proxied requests is the proxy's own.
   *
   * @param request the original request
   * @param name the header to resolve, e.g. "User-Agent"
   * @return the forwarded value if present, otherwise the direct value
   */
  private static String forwardedHeader(HttpServletRequest request, String name) {
    String forwarded = request.getHeader("X-Forwarded-" + name);
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded;
    }
    return request.getHeader(name);
  }

  /**
   * Resolves the client IP, preferring the first hop of X-Forwarded-For
   * (set by upstream proxies/load balancers) over the raw remote address.
   *
   * @param request the original request
   * @return the best-guess client IP
   */
  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
    }
    return request.getRemoteAddr();
  }
}

package com.sun.graphql.config;

import com.sun.gaia.service.UserContextHolder;
import graphql.language.Document;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Token-bucket rate limiting over POST /graphql, returning HTTP 429 when a
 * bucket is exhausted.
 *
 * Runs after the JWT filter so the caller's account id is known. Requests are
 * keyed by account id when authenticated, otherwise by client IP. The
 * operation's configured limit comes from the registry, falling back to the
 * properties default.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(
    name = "ratelimit.enabled", havingValue = "true", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE + 60)
public class RateLimitFilter extends OncePerRequestFilter {

  private static final String GRAPHQL_PATH = "/graphql";
  private static final String DEFAULT_BUCKET = "default";

  private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
  private final boolean enabled;
  private final RateLimitProperties properties;
  private final RateLimitRegistry registry;

  /**
   * Builds the filter from configuration and the operation registry.
   */
  public RateLimitFilter(RateLimitProperties properties, RateLimitRegistry registry) {
    this.enabled = properties.enabled();
    this.properties = properties;
    this.registry = registry;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    return !GRAPHQL_PATH.equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    byte[] body = request.getInputStream().readAllBytes();
    String bodyText = new String(body, StandardCharsets.UTF_8);
    String operation = operationName(bodyText);

    RateLimitBinding binding = registry.forOperation(operation).orElse(null);
    String bucketName = binding != null ? binding.bucket() : DEFAULT_BUCKET;
    RateLimitConfig config = binding != null ? binding.config() : defaultConfig();
    String key = key(request, bucketName);

    TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(
        config.capacity(), config.refillPerSecond()));
    if (!bucket.tryAcquire()) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json");
      response.setHeader("Retry-After", String.valueOf(bucket.retryAfterSeconds()));
      response.getWriter().write("{\"error\":\"Too Many Requests\"}");
      return;
    }
    filterChain.doFilter(new CachedBodyRequest(request, body), response);
  }

  /**
   * Resolves the fallback limit from the properties.
   *
   * @return the default bucket config
   */
  private RateLimitConfig defaultConfig() {
    int capacity = properties.defaultCapacity() > 0 ? properties.defaultCapacity() : 100;
    double refill = properties.defaultRefillPerSecond() > 0
        ? properties.defaultRefillPerSecond()
        : 100.0 / 60.0;
    return new RateLimitConfig(capacity, refill);
  }

  /**
   * Extracts the first operation name from a GraphQL JSON payload.
   *
   * @param body the request body
   * @return the operation name, or empty when unparseable
   */
  private static String operationName(String body) {
    String query = queryField(body);
    if (query == null || query.isBlank()) {
      return "";
    }
    try {
      Document document = new Parser().parseDocument(query);
      return document.getDefinitions().stream()
          .filter(graphql.language.OperationDefinition.class::isInstance)
          .map(graphql.language.OperationDefinition.class::cast)
          .map(graphql.language.OperationDefinition::getName)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse("");
    } catch (InvalidSyntaxException e) {
      return "";
    }
  }

  /**
   * Reads the query string from a GraphQL JSON body.
   *
   * @param body the request body
   * @return the query string, or null when absent
   */
  private static String queryField(String body) {
    if (body == null || body.isBlank()) {
      return null;
    }
    String marker = "\"query\"";
    int start = body.indexOf(marker);
    if (start < 0) {
      return null;
    }
    start += marker.length();
    while (start < body.length()
        && (Character.isWhitespace(body.charAt(start)) || body.charAt(start) == ':')) {
      start++;
    }
    if (start >= body.length() || body.charAt(start) != '"') {
      return null;
    }
    start++;
    StringBuilder query = new StringBuilder();
    boolean escaped = false;
    for (int i = start; i < body.length(); i++) {
      char c = body.charAt(i);
      if (escaped) {
        query.append(c);
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else if (c == '"') {
        return query.toString();
      } else {
        query.append(c);
      }
    }
    return null;
  }

  /**
   * Builds the rate-limit key for a request.
   *
   * @param request the servlet request
   * @param bucketName the bucket being charged
   * @return the per-account or per-IP key
   */
  private static String key(HttpServletRequest request, String bucketName) {
    if (UserContextHolder.getUserId() != null) {
      return bucketName + ":" + UserContextHolder.getUserId();
    }
    return bucketName + ":ip:" + clientIp(request);
  }

  /**
   * Resolves the client IP from proxy headers, then the remote address.
   *
   * @param request the servlet request
   * @return the client IP
   */
  private static String clientIp(HttpServletRequest request) {
    String cf = request.getHeader("CF-Connecting-IP");
    if (cf != null && !cf.isBlank()) {
      return cf.trim();
    }
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
    }
    return request.getRemoteAddr();
  }

  /**
   * Re-serves a request body that was fully read for rate limiting.
   */
  private static final class CachedBodyRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    private CachedBodyRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body;
    }

    @Override
    public ServletInputStream getInputStream() {
      ByteArrayInputStream stream = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public int read() {
          return stream.read();
        }

        @Override
        public boolean isFinished() {
          return stream.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener readListener) {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
  }
}

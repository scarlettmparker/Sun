package com.sun.graphql.audit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives each request a correlation id, puts it in the MDC for logs, and echoes
 * it on the response. Runs as the outermost filter.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  /** Header read and echoed for correlation ids. */
  public static final String HEADER = "X-Correlation-Id";
  /** Fallback header when the primary one is absent. */
  public static final String ALT_HEADER = "X-Request-Id";
  /** MDC key under which the correlation id is logged. */
  public static final String MDC_KEY = "correlationId";
  /** Request attribute carrying the resolved correlation id. */
  public static final String REQUEST_ATTRIBUTE = "audit.correlationId";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws java.io.IOException, ServletException {

    String correlationId = request.getHeader(HEADER);
    if (isBlank(correlationId)) {
      correlationId = request.getHeader(ALT_HEADER);
    }
    if (isBlank(correlationId)) {
      correlationId = UUID.randomUUID().toString();
    }
    request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
    response.setHeader(HEADER, correlationId);

    try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
      chain.doFilter(request, response);
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

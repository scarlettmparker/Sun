package com.sun.graphql.audit.filter;

import com.sun.base.audit.context.AuditContext;
import com.sun.gaia.service.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Copies the caller's id from {@link UserContextHolder} into {@link AuditContext}
 * during the request. The outer capture filter can't read UserContextHolder in
 * its finally block because the auth filter has already cleared it by then.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 51)
public class AuditIdentityFilter extends OncePerRequestFilter {

  private final AuditContext auditContext;

  public AuditIdentityFilter(AuditContext auditContext) {
    this.auditContext = auditContext;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // Null when anonymous - recorded as a null user id on the audit row.
    auditContext.setUserId(UserContextHolder.getUserId());
    chain.doFilter(request, response);
  }
}

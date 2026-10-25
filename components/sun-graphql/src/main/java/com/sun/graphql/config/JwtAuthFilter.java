package com.sun.graphql.config;

import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts the authenticated caller's account id in the user context from a
 * bearer JWT, ignoring non-active accounts so their sessions no longer work.
 */
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final AccountRepository accountRepository;

  public JwtAuthFilter(JwtService jwtService, AccountRepository accountRepository) {
    this.jwtService = jwtService;
    this.accountRepository = accountRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      UUID accountId = jwtService.authenticatedAccountId(token);
      if (accountId == null) {
        unauthorized(response);
        return;
      }
      if (accountRepository.existsByIdAndStatus(accountId, AccountStatus.ACTIVE)) {
        UserContextHolder.setUserId(accountId);
      }
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      UserContextHolder.clear();
    }
  }

  /**
   * Writes the standard unauthorized response.
   *
   * @param response the servlet response
   * @throws IOException when writing fails
   */
  private void unauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
  }
}

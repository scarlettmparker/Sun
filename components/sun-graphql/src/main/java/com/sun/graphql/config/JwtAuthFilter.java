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
 * Stamps the caller's account id into {@link UserContextHolder} from a bearer JWT.
 * Skips accounts with {@link AccountStatus#SUSPENDED} status so their sessions
 * are immediately revoked — every downstream check sees an unauthenticated user.
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
      if (jwtService.isValid(token)) {
        UUID accountId = jwtService.extractAccountId(token);
        accountRepository.findById(accountId)
            .filter(a -> a.getStatus() != AccountStatus.SUSPENDED)
            .ifPresent(a -> UserContextHolder.setUserId(accountId));
      }
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      UserContextHolder.clear();
    }
  }
}

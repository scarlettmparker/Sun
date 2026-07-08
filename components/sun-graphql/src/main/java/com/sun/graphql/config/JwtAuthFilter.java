package com.sun.graphql.config;

import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stamps the caller's account id into {@link UserContextHolder} from a bearer JWT.
 */
@Component
@Profile("!test")
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
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
        UserContextHolder.setUserId(accountId);
      }
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      UserContextHolder.clear();
    }
  }
}

package com.sun.graphql.config;

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
 * Active only under the {@code test} profile.
 */
@Component
@Profile("test")
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class TestAuthFilter extends OncePerRequestFilter {

  public static final UUID TEST_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    UserContextHolder.setUserId(TEST_USER_ID);
    try {
      filterChain.doFilter(request, response);
    } finally {
      UserContextHolder.clear();
    }
  }
}

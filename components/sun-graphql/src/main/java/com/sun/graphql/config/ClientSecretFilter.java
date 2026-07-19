package com.sun.graphql.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects any request whose X-Client-Id / X-Client-Secret pair is not known.
 */
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class ClientSecretFilter extends OncePerRequestFilter {

  private final Map<String, String> secrets;

  /**
   * Binds the configured app secrets.
   *
   * @param properties Per-app secret map from configuration.
   */
  public ClientSecretFilter(ClientSecretProperties properties) {
    this.secrets = properties.secrets() == null ? Map.of() : properties.secrets();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String id = request.getHeader("X-Client-Id");
    String secret = request.getHeader("X-Client-Secret");
    String expected = id == null ? null : secrets.get(id);
    if (!matches(expected, secret)) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Invalid client credentials\"}");
      return;
    }
    filterChain.doFilter(request, response);
  }

  /**
   * Constant-time comparison against the expected secret.
   *
   * @param expected Configured secret for the app, or null when unknown.
   * @param secret Submitted secret header value, or null when absent.
   * @return True when the app is known and the secrets match.
   */
  private boolean matches(String expected, String secret) {
    if (expected == null || expected.isBlank() || secret == null) {
      return false;
    }
    return MessageDigest.isEqual(
        secret.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
  }
}

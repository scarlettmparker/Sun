package com.sun.graphql.config;

import com.sun.gaia.model.ApiKeyEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.ApiKeyService;
import com.sun.gaia.service.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a service account from an X-Api-Key header.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "app.bypass-permissions", havingValue = "false", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE + 52)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

  private final ApiKeyService apiKeyService;
  private final AccountRepository accountRepository;

  public ApiKeyAuthFilter(ApiKeyService apiKeyService, AccountRepository accountRepository) {
    this.apiKeyService = apiKeyService;
    this.accountRepository = accountRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (UserContextHolder.getUserId() == null) {
      String apiKey = request.getHeader("X-Api-Key");
      if (apiKey != null && !apiKey.isBlank()) {
        if (!authenticate(apiKey, response)) {
          return;
        }
      }
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      UserContextHolder.clear();
    }
  }

  /**
   * Puts the resolved account into the user context on success.
   *
   * @param apiKey   the submitted key value
   * @param response the servlet response, written on failure
   * @return true when the key resolved to an active account
   */
  private boolean authenticate(String apiKey, HttpServletResponse response) throws IOException {
    ApiKeyEntity key = apiKeyService.resolve(apiKey)
        .filter(ApiKeyEntity::isEnabled)
        .orElse(null);
    boolean active = key != null
        && accountRepository.findById(key.getAccountId())
            .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
            .isPresent();
    if (!active) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Invalid API key\"}");
      return false;
    }
    UserContextHolder.setUserId(key.getAccountId());
    apiKeyService.markUsed(key.getId());
    return true;
  }
}

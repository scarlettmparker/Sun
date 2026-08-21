package com.sun.gaia.graphql.resolvers;

import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves the authenticated user from the current request.
 */
@Component
public class GaiaAuthHelper {

  private final JwtService jwtService;
  private final AccountRepository accountRepository;

  public GaiaAuthHelper(JwtService jwtService, AccountRepository accountRepository) {
    this.jwtService = jwtService;
    this.accountRepository = accountRepository;
  }

  /**
   * Resolves the user from the Authorization header and stamps the context.
   */
  public void resolveUserFromRequest() {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs == null) {
        return;
      }
      HttpServletRequest request = attrs.getRequest();
      String authHeader = request.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        if (jwtService.isValid(token)) {
          UUID accountId = jwtService.extractAccountId(token);
          accountRepository.findById(accountId)
              .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
              .ifPresent(a -> UserContextHolder.setUserId(accountId));
        }
      }
    } catch (Exception e) {
      // No servlet context available (e.g. test)
    }
  }
}

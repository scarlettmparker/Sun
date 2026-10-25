package com.sun.graphql.config;

import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final AccountService accountService;
  private final JwtService jwtService;
  private final String appBaseUrl;

  public GoogleOAuth2SuccessHandler(AccountService accountService, JwtService jwtService,
      @Value("${app.base-url:http://localhost:5178}") String appBaseUrl) {
    this.accountService = accountService;
    this.jwtService = jwtService;
    this.appBaseUrl = appBaseUrl;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {
    OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
    String email = oauthUser.getAttribute("email");
    String sub = oauthUser.getAttribute("sub");

    List<AccountEntity> accounts = (email == null) ? List.of() : accountService.findByPersonEmail(email);
    AccountEntity account = accounts.stream()
        .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
        .findFirst()
        .orElse(null);

    if (account == null) {
      getRedirectStrategy().sendRedirect(request, response, appBaseUrl + "/login?error=google");
      return;
    }
    if (account.getProvider() == null || account.getProvider().isBlank()) {
      account.setProvider("google");
      account.setProviderId(sub);
      accountService.save(account);
    }

    String token = jwtService.generateToken(account.getId(), account.getPersonId());
    getRedirectStrategy().sendRedirect(request, response,
        appBaseUrl + "/auth/callback?token=" + token);
  }
}
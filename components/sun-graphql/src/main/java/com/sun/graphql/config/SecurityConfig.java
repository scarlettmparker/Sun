package com.sun.graphql.config;

import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.EmailService;
import com.sun.gaia.service.GaiaUserDetailsService;
import com.sun.gaia.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.authentication.ott.InMemoryOneTimeTokenService;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Method-level authorization for GraphQL operations - active when permissions are
 * enforced (default). Disabled when {@code app.bypass-permissions=true}.
 */
@Configuration
@ConditionalOnProperty(name = "app.bypass-permissions", havingValue = "false", matchIfMissing = true)
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  /**
   * Leaves the HTTP layer open; access control is per-operation via
   * {@code @PreAuthorize}.
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
      GoogleOAuth2SuccessHandler successHandler,
      AccountService accountService,
      JwtService jwtService,
      EmailService emailService,
      @Value("${app.base-url:http://localhost:5178}") String appBaseUrl) throws Exception {
    AuthenticationSuccessHandler oneTimeSuccess = (request, response, authentication) -> {
      String username = authentication.getName();
      var account = accountService.findByLoginIdentifier(username).orElse(null);
      if (account == null || account.getStatus() != com.sun.gaia.model.enums.AccountStatus.ACTIVE) {
        response.sendRedirect(appBaseUrl + "/login?error=link");
        return;
      }
      String token = jwtService.generateToken(account.getId(), account.getPersonId());
      response.sendRedirect(appBaseUrl + "/auth/callback?token=" + token);
    };

    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .oauth2Login(oauth -> oauth.successHandler(successHandler))
        .oneTimeTokenLogin(ott -> ott
            .tokenGenerationSuccessHandler((request, response, oneTimeToken) -> {
              String username = oneTimeToken.getUsername();
              var account = accountService.findByLoginIdentifier(username).orElse(null);
              if (account == null || account.getStatus() != com.sun.gaia.model.enums.AccountStatus.ACTIVE) {
                return;
              }
              String link = appBaseUrl + "/login/ott?token=" + oneTimeToken.getTokenValue();
              emailService.sendMagicLinkEmail(username, link);
            })
            .authenticationSuccessHandler(oneTimeSuccess));
    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService(GaiaUserDetailsService gaiaUserDetailsService) {
    return gaiaUserDetailsService;
  }

  @Bean
  public OneTimeTokenService oneTimeTokenService() {
    return new InMemoryOneTimeTokenService();
  }
}

/**
 * Replaces SecurityConfig when permissions are bypassed - no method-security
 * annotations are evaluated, and the HTTP layer is wide open.
 */
@Configuration
@ConditionalOnProperty(name = "app.bypass-permissions", havingValue = "true")
class BypassSecurityConfig {

  @Bean
  SecurityFilterChain bypassFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/__bypass__")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
    return http.build();
  }
}

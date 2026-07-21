package com.sun.graphql.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Method-level authorization for GraphQL operations — active when permissions are
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
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
    return http.build();
  }
}

/**
 * Replaces SecurityConfig when permissions are bypassed — no method-security
 * annotations are evaluated, and the HTTP layer is wide open.
 */
@Configuration
@ConditionalOnProperty(name = "app.bypass-permissions", havingValue = "true")
class BypassSecurityConfig {

  @Bean
  SecurityFilterChain bypassFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
    return http.build();
  }
}

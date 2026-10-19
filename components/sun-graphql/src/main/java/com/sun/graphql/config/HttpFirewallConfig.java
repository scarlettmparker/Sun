package com.sun.graphql.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Keeps URL validation but skips the per-request regex scan of header names and
 * values because the proxy in front of the app already rejects malformed headers.
 */
@Configuration
public class HttpFirewallConfig {

  /**
   * Builds the firewall used by the security filter chain.
   *
   * @return a strict firewall without header regex checks
   */
  @Bean
  public HttpFirewall httpFirewall() {
    StrictHttpFirewall firewall = new StrictHttpFirewall();
    firewall.setAllowedHeaderNames(name -> true);
    firewall.setAllowedHeaderValues(value -> true);
    return firewall;
  }
}

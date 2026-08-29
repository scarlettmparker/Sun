package com.sun.base.permify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Permify connection properties shared across services.
 */
@Component
public class PermifyProps {

  private final boolean enabled;
  private final String httpEndpoint;

  public PermifyProps(
      @Value("${permify.enabled:false}") boolean enabled,
      @Value("${permify.http-endpoint:http://localhost:3477}") String httpEndpoint) {
    this.enabled = enabled;
    this.httpEndpoint = httpEndpoint;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getHttpEndpoint() {
    return httpEndpoint;
  }
}

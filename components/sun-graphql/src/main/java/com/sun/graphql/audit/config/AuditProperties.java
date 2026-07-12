package com.sun.graphql.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Audit config: enable toggle and hash-chain key. A blank chain key means an
 * unkeyed chain digest (tamper-detection only, not forgery-resistance).
 */
@ConfigurationProperties(prefix = "audit")
public record AuditProperties(
    /**
     * Whether audit rows are written at all.
     */
    boolean enabled,
    /**
     * Hash-chain configuration.
     */
    Chain chain) {

  /**
   * Base64-encoded HMAC key used to hash-chain audit rows.
   */
  public record Chain(String key) {}
}

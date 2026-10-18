package com.sun.graphql.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Audit config. A blank chain key means an unkeyed digest, and a blank
 * encryption key means redaction-only payloads.
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
    Chain chain,
    /**
     * Payload encryption configuration.
     */
    Encryption encryption) {

  /**
   * Base64-encoded HMAC key used to hash-chain audit rows.
   */
  public record Chain(String key) {}

  /**
   * Base64-encoded 256-bit AES key used to wrap per-row data keys.
   */
  public record Encryption(String kek) {}
}

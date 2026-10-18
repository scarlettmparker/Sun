package com.sun.graphql.audit.chain;

import com.sun.base.audit.entity.AuditEvent;
import com.sun.graphql.audit.config.AuditProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Computes the tamper-evidence hash for an audit row, shared by the writer and
 * the verifier so a row is hashed and checked the same way.
 */
@Component
public class AuditHashing {

  private static final Logger logger = LoggerFactory.getLogger(AuditHashing.class);

  private final AuditProperties properties;

  public AuditHashing(AuditProperties properties) {
    this.properties = properties;
  }

  /**
   * Hashes a row over the previous hash and its canonical projection.
   *
   * @param event the row to hash
   * @param prevHash the previous row's hash, or null for the first row
   * @return the hex-encoded hash
   */
  public String hash(AuditEvent event, String prevHash) {
    String input = (prevHash == null ? "" : prevHash) + "|" + canonicalise(event);
    byte[] key = chainKey();
    try {
      if (key != null) {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
      }
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to compute audit row hash", e);
    }
  }

  /**
   * Joins the hashed fields in a fixed order.
   *
   * @param event the row to project
   * @return the canonical string
   */
  public String canonicalise(AuditEvent event) {
    return join(
        event.getEventType(),
        event.getOperationName(),
        event.getOperationType(),
        event.getTargetEntity(),
        event.getTargetEntityId(),
        event.getOutcome(),
        event.getErrorMessage(),
        event.getEndpoint(),
        event.getHttpStatus(),
        event.getDurationMs(),
        event.getPayloadRedacted(),
        event.getPayloadEncrypted(),
        event.getCorrelationId(),
        event.getUserId(),
        event.getNamespace(),
        event.getUserName(),
        event.getUserRoles());
  }

  /**
   * Reports whether the hash is keyed, which separates tamper detection from
   * forgery resistance.
   *
   * @return true when a chain key is configured
   */
  public boolean isKeyed() {
    return chainKey() != null;
  }

  /**
   * Decodes the configured chain key.
   *
   * @return the HMAC key bytes, or null when unset or not valid base64
   */
  private byte[] chainKey() {
    String base64 = properties.chain() == null ? null : properties.chain().key();
    if (base64 == null || base64.isBlank()) {
      return null;
    }
    try {
      return Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      logger.warn("audit.chain.key is not valid base64; falling back to unkeyed digest");
      return null;
    }
  }

  /**
   * Joins parts with a separator, rendering null as empty.
   *
   * @param parts the values to join, in order
   * @return the joined string
   */
  private static String join(Object... parts) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) {
        sb.append('|');
      }
      sb.append(parts[i] == null ? "" : parts[i]);
    }
    return sb.toString();
  }
}

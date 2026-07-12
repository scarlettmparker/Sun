package com.sun.graphql.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.base.audit.context.AuditContext;
import com.sun.base.audit.context.AuditRequestSnapshot;
import com.sun.base.audit.entity.AuditEvent;
import com.sun.base.audit.redaction.PayloadRedactor;
import com.sun.base.audit.repository.AuditEventRepository;
import com.sun.graphql.audit.config.AuditProperties;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds audit rows from a request snapshot and saves them on a separate thread,
 * so an audit failure can't roll back the business work.
 */
@Service
public class AuditEventService {

  private static final Logger logger = LoggerFactory.getLogger(AuditEventService.class);

  /** Advisory-lock key; any constant works, it just must be stable across writes. */
  private static final long CHAIN_LOCK_KEY = 8_521_479_633L;

  private final AuditEventRepository repository;
  private final PayloadRedactor redactor;
  private final ObjectMapper objectMapper;
  private final AuditProperties properties;
  private final EntityManager entityManager;

  public AuditEventService(AuditEventRepository repository,
                           PayloadRedactor redactor,
                           ObjectMapper objectMapper,
                           AuditProperties properties,
                           EntityManager entityManager) {
    this.repository = repository;
    this.redactor = redactor;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.entityManager = entityManager;
  }

  /**
   * Builds and persists one audit row per operation in the snapshot, on the
   * auditExecutor in a fresh transaction so a persistence failure cannot roll
   * back the triggering business operation.
   *
   * @param snapshot the per-request state captured on the request thread
   * @param durationMs wall-clock request duration, recorded on each row
   */
  @Async("auditExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void persist(AuditRequestSnapshot snapshot, long durationMs) {
    if (!properties.enabled() || snapshot == null) {
      return;
    }
    try {
      // Hold the advisory lock so concurrent persists keep a linear chain.
      entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
          .setParameter("key", CHAIN_LOCK_KEY)
          .getSingleResult();
      // Seed the chain from the latest existing row globally.
      String prevHash = latestRowHash().orElse(null);

      for (AuditContext.AuditOperation op : snapshot.operations()) {
        AuditEvent event = buildEvent(snapshot, op, durationMs);
        String rowHash = computeHash(event, prevHash);
        event.setPrevHash(prevHash);
        event.setRowHash(rowHash);
        repository.save(event);
        prevHash = rowHash; // chain the next op in the same request
      }
    } catch (Exception e) {
      // Auditing must not affect the response, so swallow failures here.
      logger.error("Failed to persist audit events (correlationId={})",
          snapshot.correlationId(), e);
    }
  }

  /**
   * Builds a single audit row from a snapshot and one of its operations.
   *
   * @param snapshot the per-request state
   * @param op one captured operation
   * @param durationMs wall-clock request duration
   * @return the populated, unhashed AuditEvent
   */
  private AuditEvent buildEvent(AuditRequestSnapshot snapshot, AuditContext.AuditOperation op, long durationMs) {
    var meta = op.metadata();

    JsonNode redacted = redactor.redact(op.variables(), meta.sensitiveFields());
    String payloadRedacted = writeJsonOrEmpty(redacted);

    AuditEvent event = new AuditEvent();
    event.setCorrelationId(orRandom(snapshot.correlationId()));
    event.setUserId(snapshot.userId());
    event.setNamespace(meta.namespace());
    event.setEventType(meta.eventType());
    event.setOperationName(op.operationName());
    event.setOperationType(meta.operationType());
    event.setTargetEntity(meta.targetEntity());
    event.setTargetEntityId(op.targetEntityId());
    event.setOutcome(op.outcome());
    event.setErrorMessage(op.errorMessage());
    event.setEndpoint(safe(snapshot.endpoint()));
    event.setIpAddress(snapshot.ipAddress());
    event.setUserAgent(snapshot.userAgent());
    event.setHttpStatus(snapshot.httpStatus());
    event.setDurationMs(durationMs);
    event.setPayloadRedacted(payloadRedacted);
    return event;
  }

  private String writeJsonOrEmpty(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      logger.warn("Failed to serialise audit JSON; storing empty string", e);
      return "";
    }
  }

  private Optional<String> latestRowHash() {
    return repository.findFirstByOrderByCreatedAtDescIdDesc().map(AuditEvent::getRowHash);
  }

  /**
   * Computes the row's chain hash - HMAC-SHA256 over the previous hash and a
   * canonical projection of the row. Falls back to an unkeyed SHA-256 digest
   * when no chain key is configured (detects modification, not forgery).
   *
   * @param event the row to hash
   * @param prevHash the previous row's hash, or null for the first row
   * @return the hex-encoded hash
   */
  private String computeHash(AuditEvent event, String prevHash) {
    String canonical = canonicalise(event);
    String input = (prevHash == null ? "" : prevHash) + "|" + canonical;
    byte[] key = chainKey();
    try {
      if (key != null) {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return hex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
      }
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return hex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to compute audit row hash", e);
    }
  }

  private String canonicalise(AuditEvent e) {
    return join(
        e.getEventType(),
        e.getOperationName(),
        e.getOperationType(),
        e.getNamespace(),
        e.getTargetEntity(),
        e.getTargetEntityId(),
        e.getOutcome(),
        e.getErrorMessage(),
        e.getEndpoint(),
        e.getHttpStatus(),
        e.getDurationMs(),
        e.getPayloadRedacted(),
        e.getCorrelationId(),
        e.getUserId());
  }

  private byte[] chainKey() {
    String base64 = properties.chain() == null ? null : properties.chain().key();
    if (base64 == null || base64.isBlank()) {
      return null;
    }
    try {
      return java.util.Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      logger.warn("audit.chain.key is not valid base64; falling back to unkeyed digest");
      return null;
    }
  }

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

  private static String hex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(Character.forDigit((b >> 4) & 0xF, 16));
      sb.append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }

  private static UUID orRandom(UUID value) {
    return value == null ? UUID.randomUUID() : value;
  }

  private static String safe(String value) {
    return value == null ? "unknown" : value;
  }
}

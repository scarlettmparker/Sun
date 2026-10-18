package com.sun.graphql.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.base.audit.context.AuditContext;
import com.sun.base.audit.context.AuditRequestSnapshot;
import com.sun.base.audit.entity.AuditEvent;
import com.sun.base.audit.redaction.PayloadRedactor;
import com.sun.base.audit.repository.AuditEventRepository;
import com.sun.graphql.audit.chain.AuditHashing;
import com.sun.graphql.audit.config.AuditProperties;
import com.sun.graphql.audit.encryption.AuditPayloadEncryptor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds audit rows from a request snapshot and saves them on a separate thread,
 * so an audit failure cannot roll back the business work.
 */
@Service
public class AuditEventService {

  private static final Logger logger = LoggerFactory.getLogger(AuditEventService.class);

  private final AuditEventRepository repository;
  private final PayloadRedactor redactor;
  private final AuditPayloadEncryptor encryptor;
  private final AuditHashing hashing;
  private final ObjectMapper objectMapper;
  private final AuditProperties properties;

  public AuditEventService(AuditEventRepository repository,
                           PayloadRedactor redactor,
                           AuditPayloadEncryptor encryptor,
                           AuditHashing hashing,
                           ObjectMapper objectMapper,
                           AuditProperties properties) {
    this.repository = repository;
    this.redactor = redactor;
    this.encryptor = encryptor;
    this.hashing = hashing;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  /**
   * Persists one audit row per operation in the snapshot, on the audit executor
   * in a fresh transaction.
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
      String prevHash = latestRowHash(snapshot).orElse(null);

      for (AuditContext.AuditOperation op : snapshot.operations()) {
        AuditEvent event = buildEvent(snapshot, op, durationMs);
        String rowHash = hashing.hash(event, prevHash);
        event.setPrevHash(prevHash);
        event.setRowHash(rowHash);
        repository.save(event);
        prevHash = rowHash;
      }
    } catch (Exception e) {
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

    JsonNode redacted = redactor.redact(op.variables(), meta.sensitiveFields(), meta.encryptFields());
    String payloadRedacted = writeJsonOrEmpty(redacted);
    String payloadEncrypted = encryptSubset(op.variables(), meta.encryptFields());

    String payloadBefore = null;
    String payloadBeforeEncrypted = null;
    if (op.beforePayload() != null) {
      JsonNode redactedBefore = redactor.redact(op.beforePayload(), meta.sensitiveFields(), meta.encryptFields());
      payloadBefore = writeJsonOrEmpty(redactedBefore);
      payloadBeforeEncrypted = encryptSubset(op.beforePayload(), meta.encryptFields());
    }

    AuditEvent event = new AuditEvent();
    event.setCorrelationId(orRandom(snapshot.correlationId()));
    event.setUserId(snapshot.userId());
    event.setUserName(snapshot.userName());
    event.setUserRoles(writeJsonOrEmpty(snapshot.userRoles() == null ? List.of() : snapshot.userRoles()));
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
    event.setPayloadEncrypted(payloadEncrypted);
    event.setPayloadBefore(payloadBefore);
    event.setPayloadBeforeEncrypted(payloadBeforeEncrypted);
    return event;
  }

  /**
   * Serialises a value to JSON, tolerating values Jackson cannot encode.
   *
   * @param value the value to serialise
   * @return the JSON string, an empty string when serialisation fails, or null for null input
   */
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

  /**
   * Extracts the encrypt-routed fields at any depth, serialises them, and
   * envelope-encrypts the result.
   *
   * @param variables the raw GraphQL variables or REST body
   * @param encryptFields field names to retain under encryption
   * @return the encrypted envelope, or null when nothing sensitive is present
   */
  private String encryptSubset(Object variables, Set<String> encryptFields) {
    if (encryptFields == null || encryptFields.isEmpty() || variables == null) {
      return null;
    }
    JsonNode root = objectMapper.valueToTree(variables);
    Map<String, JsonNode> subset = new LinkedHashMap<>();
    collectFields(root, encryptFields, subset);
    if (subset.isEmpty()) {
      return null;
    }
    try {
      return encryptor.encrypt(objectMapper.writeValueAsString(subset));
    } catch (Exception e) {
      logger.warn("Failed to serialise subset for encryption; skipping", e);
      return null;
    }
  }

  /**
   * Collects matching fields from an object tree into a flat map, walking nested
   * objects and arrays.
   *
   * @param node the node to search
   * @param encryptFields field names to match, case-insensitively
   * @param into the map to collect matches into
   */
  private void collectFields(JsonNode node, Set<String> encryptFields, Map<String, JsonNode> into) {
    if (node == null || !node.isObject()) {
      return;
    }
    node.fields().forEachRemaining(entry -> {
      String name = entry.getKey();
      if (encryptFields.stream().anyMatch(f -> f.equalsIgnoreCase(name))) {
        into.put(name, entry.getValue());
      } else {
        JsonNode child = entry.getValue();
        if (child != null && child.isObject()) {
          collectFields(child, encryptFields, into);
        } else if (child != null && child.isArray()) {
          for (JsonNode element : child) {
            collectFields(element, encryptFields, into);
          }
        }
      }
    });
  }

  /**
   * Finds the row hash the next row should chain onto, scoped by namespace.
   *
   * @param snapshot the per-request state
   * @return the latest row hash in the namespace, or empty when none exists
   */
  private Optional<String> latestRowHash(AuditRequestSnapshot snapshot) {
    String namespace = snapshot.operations().isEmpty()
        ? null
        : snapshot.operations().get(0).metadata().namespace();
    Optional<AuditEvent> last = namespace == null
        ? repository.findFirstByNamespaceIsNullOrderByCreatedAtDescIdDesc()
        : repository.findFirstByNamespaceOrderByCreatedAtDescIdDesc(namespace);
    return last.map(AuditEvent::getRowHash);
  }

  /**
   * Falls back to a random id when none was captured, since the column is not null.
   *
   * @param value the captured correlation id, or null
   * @return the id, or a random one
   */
  private static UUID orRandom(UUID value) {
    return value == null ? UUID.randomUUID() : value;
  }

  /**
   * Substitutes a placeholder for a missing required string column.
   *
   * @param value the value, or null
   * @return the value, or "unknown"
   */
  private static String safe(String value) {
    return value == null ? "unknown" : value;
  }
}

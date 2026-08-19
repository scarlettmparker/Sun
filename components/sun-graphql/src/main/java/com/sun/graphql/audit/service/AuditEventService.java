package com.sun.graphql.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.base.audit.context.AuditContext;
import com.sun.base.audit.context.AuditRequestSnapshot;
import com.sun.base.audit.entity.AuditEvent;
import com.sun.base.audit.redaction.PayloadRedactor;
import com.sun.base.audit.repository.AuditEventRepository;
import com.sun.graphql.audit.config.AuditProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit rows asynchronously from request snapshots.
 */
@Service
public class AuditEventService {

  private static final Logger logger = LoggerFactory.getLogger(AuditEventService.class);

  private final AuditEventRepository repository;
  private final PayloadRedactor redactor;
  private final ObjectMapper objectMapper;
  private final AuditProperties properties;

  public AuditEventService(AuditEventRepository repository,
                           PayloadRedactor redactor,
                           ObjectMapper objectMapper,
                           AuditProperties properties) {
    this.repository = repository;
    this.redactor = redactor;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  /**
   * Persists one audit row per operation in a fresh transaction.
   *
   * @param snapshot per-request state captured on the request thread
   * @param durationMs wall-clock request duration
   */
  @Async("auditExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void persist(AuditRequestSnapshot snapshot, long durationMs) {
    if (!properties.enabled() || snapshot == null) {
      return;
    }
    try {
      List<AuditEvent> batch = new ArrayList<>(snapshot.operations().size());

      for (AuditContext.AuditOperation op : snapshot.operations()) {
        AuditEvent event = buildEvent(snapshot, op, durationMs);
        event.setRowHash(UUID.randomUUID().toString());
        batch.add(event);
      }

      repository.saveAll(batch);
    } catch (Exception e) {
      logger.error("Failed to persist audit events (correlationId={})",
          snapshot.correlationId(), e);
    }
  }

  /**
   * Populates a single audit row from a snapshot and one operation.
   *
   * @param snapshot per-request state
   * @param op one captured operation
   * @param durationMs wall-clock request duration
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

  private static UUID orRandom(UUID value) {
    return value == null ? UUID.randomUUID() : value;
  }

  private static String safe(String value) {
    return value == null ? "unknown" : value;
  }
}

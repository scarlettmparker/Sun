package com.sun.base.audit.entity;

import com.sun.base.audit.enums.AuditOutcome;
import com.sun.base.audit.enums.OperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One immutable audit row. Append-only.
 */
@Entity
@Table(name = "audit_events")
@EntityListeners(AuditingEntityListener.class)
public class AuditEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp")
  private LocalDateTime createdAt;

  @Column(name = "correlation_id", nullable = false, updatable = false)
  private UUID correlationId;

  /** Null for anonymous or system events. */
  @Column(name = "user_id", updatable = false, columnDefinition = "uuid")
  private UUID userId;

  /** Sub-service the operation came from, e.g. HADES. */
  @Column(name = "namespace", updatable = false, columnDefinition = "text")
  private String namespace;

  @Column(name = "event_type", nullable = false, updatable = false, columnDefinition = "text")
  private String eventType;

  @Column(name = "operation_name", updatable = false, columnDefinition = "text")
  private String operationName;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false, updatable = false, columnDefinition = "text")
  private OperationType operationType;

  @Column(name = "target_entity", updatable = false, columnDefinition = "text")
  private String targetEntity;

  @Column(name = "target_entity_id", updatable = false, columnDefinition = "uuid")
  private UUID targetEntityId;

  @Enumerated(EnumType.STRING)
  @Column(name = "outcome", nullable = false, updatable = false, columnDefinition = "text")
  private AuditOutcome outcome;

  @Column(name = "error_message", updatable = false, columnDefinition = "text")
  private String errorMessage;

  @Column(name = "endpoint", nullable = false, updatable = false, columnDefinition = "text")
  private String endpoint;

  @Column(name = "ip_address", updatable = false, columnDefinition = "text")
  private String ipAddress;

  @Column(name = "user_agent", updatable = false, columnDefinition = "text")
  private String userAgent;

  @Column(name = "http_status", nullable = false, updatable = false)
  private int httpStatus;

  @Column(name = "duration_ms", updatable = false)
  private Long durationMs;

  /** Masked request body/variables, safe to store as plain text. */
  @Column(name = "payload_redacted", updatable = false, columnDefinition = "text")
  private String payloadRedacted;

  @Column(name = "prev_hash", updatable = false, columnDefinition = "text")
  private String prevHash;

  @Column(name = "row_hash", nullable = false, updatable = false, columnDefinition = "text")
  private String rowHash;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public UUID getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(UUID correlationId) {
    this.correlationId = correlationId;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getOperationName() {
    return operationName;
  }

  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }

  public String getTargetEntity() {
    return targetEntity;
  }

  public void setTargetEntity(String targetEntity) {
    this.targetEntity = targetEntity;
  }

  public UUID getTargetEntityId() {
    return targetEntityId;
  }

  public void setTargetEntityId(UUID targetEntityId) {
    this.targetEntityId = targetEntityId;
  }

  public AuditOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(AuditOutcome outcome) {
    this.outcome = outcome;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  public Long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }

  public String getPayloadRedacted() {
    return payloadRedacted;
  }

  public void setPayloadRedacted(String payloadRedacted) {
    this.payloadRedacted = payloadRedacted;
  }

  public String getPrevHash() {
    return prevHash;
  }

  public void setPrevHash(String prevHash) {
    this.prevHash = prevHash;
  }

  public String getRowHash() {
    return rowHash;
  }

  public void setRowHash(String rowHash) {
    this.rowHash = rowHash;
  }
}

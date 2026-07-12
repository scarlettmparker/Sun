package com.sun.base.audit.context;

import com.sun.base.audit.enums.AuditOutcome;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Holds the audit data for one request. Shared by the filters and the GraphQL
 * hook via a ThreadLocal; the audit filter clears it once the data is copied off.
 */
@Component
public class AuditContext {

  /**
   * One captured operation.
   */
  public record AuditOperation(
      /**
       * The field or endpoint that ran.
       */
      String operationName,
      /**
       * Resolved audit metadata.
       */
      OperationMetadata metadata,
      /**
       * The raw request variables or body.
       */
      Object variables,
      /**
       * The affected entity id, if known.
       */
      UUID targetEntityId,
      /**
       * Success, failure, or unauthorized.
       */
      AuditOutcome outcome,
      /**
       * Error detail on failure.
       */
      String errorMessage) {}

  private static final class AuditState {
    final List<AuditOperation> operations = new ArrayList<>();
    UUID correlationId;
    Instant startedAt;
    String endpoint;
    String method;
    String ipAddress;
    String userAgent;
    int httpStatus;
    UUID userId;
  }

  private final ThreadLocal<AuditState> holder = new ThreadLocal<>();

  /**
   * Starts collecting state for the current request thread.
   */
  public void begin() {
    holder.set(new AuditState());
  }

  /**
   * Discards state for the current request thread.
   */
  public void clear() {
    holder.remove();
  }

  private AuditState state() {
    AuditState s = holder.get();
    if (s == null) {
      // Defensive: ensure a state exists even if begin() was skipped.
      s = new AuditState();
      holder.set(s);
    }
    return s;
  }

  /**
   * Records one GraphQL operation (or synthetic REST operation) for the request.
   *
   * @param operation the captured operation
   */
  public void addOperation(AuditOperation operation) {
    state().operations.add(operation);
  }

  /**
   * @return the operations captured for the current request so far
   */
  public List<AuditOperation> operations() {
    return state().operations;
  }

  public UUID getCorrelationId() {
    return state().correlationId;
  }

  public void setCorrelationId(UUID correlationId) {
    state().correlationId = correlationId;
  }

  public Instant getStartedAt() {
    return state().startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    state().startedAt = startedAt;
  }

  public String getEndpoint() {
    return state().endpoint;
  }

  public void setEndpoint(String endpoint) {
    state().endpoint = endpoint;
  }

  public String getMethod() {
    return state().method;
  }

  public void setMethod(String method) {
    state().method = method;
  }

  public String getIpAddress() {
    return state().ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    state().ipAddress = ipAddress;
  }

  public String getUserAgent() {
    return state().userAgent;
  }

  public void setUserAgent(String userAgent) {
    state().userAgent = userAgent;
  }

  public int getHttpStatus() {
    return state().httpStatus;
  }

  public void setHttpStatus(int httpStatus) {
    state().httpStatus = httpStatus;
  }

  public UUID getUserId() {
    return state().userId;
  }

  public void setUserId(UUID userId) {
    state().userId = userId;
  }
}

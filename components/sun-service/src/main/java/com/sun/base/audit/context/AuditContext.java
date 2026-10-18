package com.sun.base.audit.context;

import com.sun.base.audit.enums.AuditOutcome;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Holds the audit data for one request on a ThreadLocal shared by the filters
 * and the GraphQL hook.
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
       * The request variables or body.
       */
      Object variables,
      /**
       * The prior entity state for an edit, or null.
       */
      Object beforePayload,
      /**
       * The affected entity id, if known.
       */
      UUID targetEntityId,
      /**
       * Success or failure.
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
    String userName;
    List<String> userRoles;
    Object pendingArguments;
    String pendingField;
    Object pendingBeforePayload;
    boolean pendingExcluded;
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

  /**
   * Reports whether the current request thread is collecting state.
   */
  public boolean isActive() {
    return holder.get() != null;
  }

  private AuditState state() {
    AuditState s = holder.get();
    if (s == null) {
      s = new AuditState();
      holder.set(s);
    }
    return s;
  }

  /**
   * Records one operation for the request.
   */
  public void addOperation(AuditOperation operation) {
    state().operations.add(operation);
  }

  /**
   * Returns the operations captured for the request so far.
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

  public String getUserName() {
    return state().userName;
  }

  public void setUserName(String userName) {
    state().userName = userName;
  }

  public List<String> getUserRoles() {
    return state().userRoles;
  }

  public void setUserRoles(List<String> userRoles) {
    state().userRoles = userRoles;
  }

  public Object getPendingArguments() {
    return state().pendingArguments;
  }

  public void setPendingArguments(Object arguments) {
    state().pendingArguments = arguments;
  }

  public String getPendingField() {
    return state().pendingField;
  }

  public void setPendingField(String field) {
    state().pendingField = field;
  }

  public Object getPendingBeforePayload() {
    return state().pendingBeforePayload;
  }

  public void setPendingBeforePayload(Object beforePayload) {
    state().pendingBeforePayload = beforePayload;
  }

  public boolean isPendingExcluded() {
    return state().pendingExcluded;
  }

  public void setPendingExcluded(boolean excluded) {
    state().pendingExcluded = excluded;
  }
}

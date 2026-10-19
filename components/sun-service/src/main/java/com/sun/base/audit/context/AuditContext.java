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
    return holder.get();
  }

  /**
   * Records one operation for the request.
   */
  public void addOperation(AuditOperation operation) {
    AuditState s = state();
    if (s != null) {
      s.operations.add(operation);
    }
  }

  /**
   * Returns the operations captured for the request so far.
   */
  public List<AuditOperation> operations() {
    AuditState s = state();
    return s == null ? List.of() : s.operations;
  }

  public UUID getCorrelationId() {
    AuditState s = state();
    return s == null ? null : s.correlationId;
  }

  public void setCorrelationId(UUID correlationId) {
    AuditState s = state();
    if (s != null) {
      s.correlationId = correlationId;
    }
  }

  public Instant getStartedAt() {
    AuditState s = state();
    return s == null ? null : s.startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    AuditState s = state();
    if (s != null) {
      s.startedAt = startedAt;
    }
  }

  public String getEndpoint() {
    AuditState s = state();
    return s == null ? null : s.endpoint;
  }

  public void setEndpoint(String endpoint) {
    AuditState s = state();
    if (s != null) {
      s.endpoint = endpoint;
    }
  }

  public String getMethod() {
    AuditState s = state();
    return s == null ? null : s.method;
  }

  public void setMethod(String method) {
    AuditState s = state();
    if (s != null) {
      s.method = method;
    }
  }

  public String getIpAddress() {
    AuditState s = state();
    return s == null ? null : s.ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    AuditState s = state();
    if (s != null) {
      s.ipAddress = ipAddress;
    }
  }

  public String getUserAgent() {
    AuditState s = state();
    return s == null ? null : s.userAgent;
  }

  public void setUserAgent(String userAgent) {
    AuditState s = state();
    if (s != null) {
      s.userAgent = userAgent;
    }
  }

  public int getHttpStatus() {
    AuditState s = state();
    return s == null ? 0 : s.httpStatus;
  }

  public void setHttpStatus(int httpStatus) {
    AuditState s = state();
    if (s != null) {
      s.httpStatus = httpStatus;
    }
  }

  public UUID getUserId() {
    AuditState s = state();
    return s == null ? null : s.userId;
  }

  public void setUserId(UUID userId) {
    AuditState s = state();
    if (s != null) {
      s.userId = userId;
    }
  }

  public String getUserName() {
    AuditState s = state();
    return s == null ? null : s.userName;
  }

  public void setUserName(String userName) {
    AuditState s = state();
    if (s != null) {
      s.userName = userName;
    }
  }

  public List<String> getUserRoles() {
    AuditState s = state();
    return s == null ? null : s.userRoles;
  }

  public void setUserRoles(List<String> userRoles) {
    AuditState s = state();
    if (s != null) {
      s.userRoles = userRoles;
    }
  }

  public Object getPendingArguments() {
    AuditState s = state();
    return s == null ? null : s.pendingArguments;
  }

  public void setPendingArguments(Object arguments) {
    AuditState s = state();
    if (s != null) {
      s.pendingArguments = arguments;
    }
  }

  public String getPendingField() {
    AuditState s = state();
    return s == null ? null : s.pendingField;
  }

  public void setPendingField(String field) {
    AuditState s = state();
    if (s != null) {
      s.pendingField = field;
    }
  }

  public Object getPendingBeforePayload() {
    AuditState s = state();
    return s == null ? null : s.pendingBeforePayload;
  }

  public void setPendingBeforePayload(Object beforePayload) {
    AuditState s = state();
    if (s != null) {
      s.pendingBeforePayload = beforePayload;
    }
  }

  public boolean isPendingExcluded() {
    AuditState s = state();
    return s != null && s.pendingExcluded;
  }

  public void setPendingExcluded(boolean excluded) {
    AuditState s = state();
    if (s != null) {
      s.pendingExcluded = excluded;
    }
  }
}

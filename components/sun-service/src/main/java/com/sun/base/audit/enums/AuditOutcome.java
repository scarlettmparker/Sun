package com.sun.base.audit.enums;

/**
 * Outcome of an audited operation.
 */
public enum AuditOutcome {
  /**
   * The operation completed.
   */
  SUCCESS,
  /**
   * The operation failed, including requests rejected before business logic.
   */
  FAILURE
}

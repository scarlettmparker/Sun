package com.sun.base.audit.enums;

/**
 * Result of an audited operation.
 */
public enum AuditOutcome {
  /**
   * The operation completed successfully.
   */
  SUCCESS,
  /**
   * The operation failed or threw.
   */
  FAILURE,
  /**
   * The caller was not permitted to run the operation.
   */
  UNAUTHORIZED
}

package com.sun.base.audit.enums;

/**
 * Kind of operation an audit row records.
 */
public enum OperationType {
  /**
   * A GraphQL query (read).
   */
  QUERY,
  /**
   * A GraphQL mutation (write).
   */
  MUTATION,
  /**
   * A plain REST endpoint.
   */
  REST
}

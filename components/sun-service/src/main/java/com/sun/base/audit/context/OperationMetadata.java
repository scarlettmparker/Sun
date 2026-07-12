package com.sun.base.audit.context;

import com.sun.base.audit.enums.OperationType;
import java.util.Set;

/**
 * Audit metadata for a single GraphQL operation or REST endpoint.
 */
public record OperationMetadata(
    /**
     * Stable event-type label, e.g. HADES_CREATEANNOTATION.
     */
    String eventType,
    /**
     * QUERY, MUTATION, or REST.
     */
    OperationType operationType,
    /**
     * The sub-service the operation came from, e.g. HADES.
     */
    String namespace,
    /**
     * The domain entity acted on, or null when unknown.
     */
    String targetEntity,
    /**
     * Field names to mask in the stored payload.
     */
    Set<String> sensitiveFields) {

  /**
   * Builds a fallback metadata for unmapped operations or endpoints.
   *
   * @param operationType the operation type
   * @param targetEntity the target entity, or null
   * @return unknown-event metadata
   */
  public static OperationMetadata unknown(OperationType operationType, String targetEntity) {
    return new OperationMetadata("UNKNOWN", operationType, null, targetEntity, Set.of());
  }
}

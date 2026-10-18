package com.sun.base.audit.context;

import com.sun.base.audit.enums.OperationType;
import java.util.Set;

/**
 * Audit metadata for one operation.
 */
public record OperationMetadata(
    /**
     * Event label, e.g. HADES_CREATEANNOTATION.
     */
    String eventType,
    /**
     * QUERY, MUTATION or REST.
     */
    OperationType operationType,
    /**
     * Sub-service the operation came from, e.g. HADES.
     */
    String namespace,
    /**
     * Domain entity acted on, or null when unknown.
     */
    String targetEntity,
    /**
     * Field names masked in the stored payload.
     */
    Set<String> sensitiveFields,
    /**
     * Field names stripped from the payload and stored encrypted.
     */
    Set<String> encryptFields) {

  /**
   * Builds metadata for an operation with no declared entry.
   *
   * @param operationType the operation type
   * @param targetEntity the entity acted on, or null
   * @return metadata carrying an unknown event label
   */
  public static OperationMetadata unknown(OperationType operationType, String targetEntity) {
    return new OperationMetadata("UNKNOWN", operationType, null, targetEntity, Set.of(), Set.of());
  }
}

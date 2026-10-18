package com.sun.base.audit.snapshot;

import com.sun.base.audit.annotation.Audited;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Loads the prior state of one entity type for before-and-after auditing, keyed
 * by the entity name used in {@link Audited#entity()}.
 */
public interface AuditSnapshotLoader {

  /**
   * Returns the domain entity name this loader serves.
   *
   * @return the entity name, e.g. ReaderAnnotation
   */
  String entity();

  /**
   * Loads the editable fields of an entity as a plain map, safe to serialise off
   * the request thread.
   *
   * @param id the entity id
   * @return the prior state, or empty when the entity does not exist
   */
  Optional<Map<String, Object>> load(UUID id);
}

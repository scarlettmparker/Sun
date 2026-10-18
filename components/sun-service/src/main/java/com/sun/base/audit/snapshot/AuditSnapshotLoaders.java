package com.sun.base.audit.snapshot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Indexes every {@link AuditSnapshotLoader} by entity name.
 */
@Component
public class AuditSnapshotLoaders {

  private final Map<String, AuditSnapshotLoader> byEntity;

  public AuditSnapshotLoaders(List<AuditSnapshotLoader> loaders) {
    this.byEntity = loaders.stream()
        .collect(Collectors.toUnmodifiableMap(AuditSnapshotLoader::entity, Function.identity()));
  }

  /**
   * Loads the prior state of an entity.
   *
   * @param entity the entity name from the audited annotation
   * @param id the entity id
   * @return the prior state, or empty when unsupported or absent
   */
  public Optional<Map<String, Object>> load(String entity, UUID id) {
    if (entity == null || id == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(byEntity.get(entity)).flatMap(loader -> loader.load(id));
  }
}

package com.sun.gaia.graphql.mappers;

import java.time.ZoneOffset;
import com.sun.gaia.codegen.types.ApiKey;
import com.sun.gaia.model.ApiKeyEntity;
import org.springframework.stereotype.Component;

/**
 * Converts API key entities to their GraphQL representation.
 */
@Component
public class ApiKeyMapper {

  /**
   * Maps a single entity to a GraphQL type.
   *
   * @param entity the persisted entity
   * @return the GraphQL ApiKey
   */
  public ApiKey map(ApiKeyEntity entity) {
    ApiKey.Builder builder = ApiKey.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName())
        .keyPrefix(entity.getKeyPrefix())
        .enabled(entity.isEnabled())
        .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC))
        .updatedAt(entity.getLastUpdatedAt() == null ? null : entity.getLastUpdatedAt().atOffset(ZoneOffset.UTC));
    if (entity.getLastUsedAt() == null ? null : entity.getLastUsedAt().atOffset(ZoneOffset.UTC) != null) {
      builder.lastUsedAt(entity.getLastUsedAt() == null ? null : entity.getLastUsedAt().atOffset(ZoneOffset.UTC));
    }
    return builder.build();
  }
}

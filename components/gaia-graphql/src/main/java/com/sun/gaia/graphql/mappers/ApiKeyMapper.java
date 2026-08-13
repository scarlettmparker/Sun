package com.sun.gaia.graphql.mappers;

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
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt());
    if (entity.getLastUsedAt() != null) {
      builder.lastUsedAt(entity.getLastUsedAt());
    }
    return builder.build();
  }
}

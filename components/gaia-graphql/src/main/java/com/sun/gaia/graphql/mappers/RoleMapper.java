package com.sun.gaia.graphql.mappers;

import com.sun.gaia.codegen.types.Role;
import com.sun.gaia.model.RoleEntity;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting role entities to GraphQL types.
 */
@Component
public class RoleMapper {

  private static final Logger logger = LoggerFactory.getLogger(RoleMapper.class);

  /**
   * Maps a role entity to the GraphQL Role type.
   *
   * @param entity the role entity
   * @return the GraphQL Role
   */
  public Role map(RoleEntity entity) {
    logger.debug("Mapping role {}", entity.getName());

    Role role = Role.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName())
        .description(entity.getDescription())
        .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC))
        .updatedAt(entity.getLastUpdatedAt() == null ? null : entity.getLastUpdatedAt().atOffset(ZoneOffset.UTC))
        .build();

    logger.debug("Mapped role {}", role.getId());
    return role;
  }
}

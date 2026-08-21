package com.sun.gaia.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.gaia.codegen.types.Role;
import com.sun.gaia.model.RoleEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoleMapperTest {

  private final RoleMapper mapper = new RoleMapper();

  @Test
  void map_entityToGraphQL() {
    UUID id = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime lastUpdatedAt = LocalDateTime.now();

    RoleEntity entity = new RoleEntity();
    entity.setId(id);
    entity.setName("admin");
    entity.setDescription("desc");
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(lastUpdatedAt);

    Role result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getName()).isEqualTo("admin");
    assertThat(result.getDescription()).isEqualTo("desc");
    assertThat(result.getCreatedAt()).isNotNull();
    assertThat(result.getUpdatedAt()).isNotNull();
  }

  @Test
  void map_nullTimestampsReturnsNull() {
    RoleEntity entity = new RoleEntity();
    entity.setId(UUID.randomUUID());
    entity.setName("admin");
    entity.setDescription("desc");
    entity.setCreatedAt(null);
    entity.setLastUpdatedAt(null);

    Role result = mapper.map(entity);

    assertThat(result.getCreatedAt()).isNull();
    assertThat(result.getUpdatedAt()).isNull();
  }
}

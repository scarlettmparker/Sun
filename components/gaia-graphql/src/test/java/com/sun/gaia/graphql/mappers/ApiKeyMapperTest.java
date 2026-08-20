package com.sun.gaia.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.gaia.codegen.types.ApiKey;
import com.sun.gaia.model.ApiKeyEntity;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiKeyMapperTest {

  private final ApiKeyMapper mapper = new ApiKeyMapper();

  @Test
  void map_entityToGraphQL() {
    UUID id = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime lastUsedAt = LocalDateTime.now();

    ApiKeyEntity entity = new ApiKeyEntity();
    entity.setId(id);
    entity.setName("bot");
    entity.setKeyPrefix("ns_ab12cd34");
    entity.setEnabled(true);
    entity.setLastUsedAt(lastUsedAt);
    entity.setCreatedAt(createdAt);

    ApiKey result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getName()).isEqualTo("bot");
    assertThat(result.getKeyPrefix()).isEqualTo("ns_ab12cd34");
    assertThat(result.getEnabled()).isTrue();
    assertThat(result.getLastUsedAt()).isEqualTo(lastUsedAt);
    assertThat(result.getCreatedAt()).isEqualTo(createdAt.atOffset(ZoneOffset.UTC));
  }

  @Test
  void map_omitsLastUsedAtWhenNeverUsed() {
    ApiKeyEntity entity = new ApiKeyEntity();
    entity.setId(UUID.randomUUID());
    entity.setName("bot");
    entity.setKeyPrefix("ns_ab12cd34");

    ApiKey result = mapper.map(entity);

    assertThat(result.getLastUsedAt()).isNull();
  }
}

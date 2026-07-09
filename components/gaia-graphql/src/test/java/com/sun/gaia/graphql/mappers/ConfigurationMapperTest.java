package com.sun.gaia.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.gaia.codegen.types.Configuration;
import com.sun.gaia.model.ConfigurationEntity;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfigurationMapperTest {

  private final ConfigurationMapper mapper = new ConfigurationMapper();

  @Test
  void map_mapsAllFields() {
    ConfigurationEntity entity = new ConfigurationEntity();
    entity.setId(UUID.randomUUID());
    entity.setName("react-app-themes");
    entity.setDescription("Default themes");
    entity.setEnabled(true);
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("users", java.util.List.of());
    entity.setContent(content);
    entity.setLastAppliedAt(LocalDateTime.now());
    entity.setLastApplyError(null);

    Configuration result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(entity.getId().toString());
    assertThat(result.getName()).isEqualTo("react-app-themes");
    assertThat(result.getDescription()).isEqualTo("Default themes");
    assertThat(result.getEnabled()).isTrue();
    assertThat(result.getContent()).isEqualTo(content);
    assertThat(result.getLastAppliedAt()).isEqualTo(entity.getLastAppliedAt());
    assertThat(result.getLastApplyError()).isNull();
  }
}

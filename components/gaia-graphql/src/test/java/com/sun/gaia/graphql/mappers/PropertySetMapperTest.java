package com.sun.gaia.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.gaia.codegen.types.PropertySetEntry;
import com.sun.gaia.codegen.types.PropertySetSchema;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.model.PropertySetSchemaEntity;
import com.sun.gaia.model.enums.EntryStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PropertySetMapperTest {

  private final PropertySetMapper mapper = new PropertySetMapper();

  @Test
  void map_entryMapsAllFields() {
    PropertySetEntryEntity entity = new PropertySetEntryEntity();
    entity.setId(UUID.randomUUID());
    entity.setOwnerKey("ReactApp");
    entity.setPropertySet("themes");
    entity.setEntryName("greek");
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("primary", "#1d4ed8");
    entity.setValues(values);
    entity.setConfigurable(true);
    entity.setStatus(EntryStatus.ACTIVE);

    PropertySetEntry result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(entity.getId().toString());
    assertThat(result.getOwnerKey()).isEqualTo("ReactApp");
    assertThat(result.getPropertySet()).isEqualTo("themes");
    assertThat(result.getEntryName()).isEqualTo("greek");
    assertThat(result.getValues()).isEqualTo(values);
    assertThat(result.getConfigurable()).isTrue();
    assertThat(result.getStatus()).isEqualTo(EntryStatus.ACTIVE);
  }

  @Test
  void map_schemaMapsAllFields() {
    PropertySetSchemaEntity entity = new PropertySetSchemaEntity();
    entity.setId(UUID.randomUUID());
    entity.setOwnerKey("ReactApp");
    entity.setName("themes");
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("primary", Map.of("type", "color", "required", true));
    entity.setProperties(properties);
    entity.setConfigurable(true);
    entity.setStatus(EntryStatus.ACTIVE);

    PropertySetSchema result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(entity.getId().toString());
    assertThat(result.getOwnerKey()).isEqualTo("ReactApp");
    assertThat(result.getName()).isEqualTo("themes");
    assertThat(result.getProperties()).isEqualTo(properties);
    assertThat(result.getConfigurable()).isTrue();
    assertThat(result.getStatus()).isEqualTo(EntryStatus.ACTIVE);
  }
}

package com.sun.briareus.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.briareus.codegen.types.BlogPropertySet;
import com.sun.briareus.codegen.types.BlogPropertySetSchema;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.model.PropertySetSchemaEntity;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlogPropertySetMapperTest {

  private final BlogPropertySetMapper mapper = new BlogPropertySetMapper();

  @Test
  void mapSchema_shouldMapAllFields() {
    PropertySetSchemaEntity entity = new PropertySetSchemaEntity();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setOwnerKey("Blog");
    entity.setName("review-attributes");
    entity.setProperties(Map.of("rating", Map.of("type", "number")));

    BlogPropertySetSchema result = mapper.mapSchema(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getOwnerKey()).isEqualTo("Blog");
    assertThat(result.getName()).isEqualTo("review-attributes");
    assertThat(result.getProperties()).isEqualTo(entity.getProperties());
  }

  @Test
  void map_shouldMapSchemaAndValues() {
    PropertySetSchemaEntity schema = new PropertySetSchemaEntity();
    schema.setId(UUID.randomUUID());
    schema.setOwnerKey("Blog");
    schema.setName("review-attributes");
    schema.setProperties(Map.of("rating", Map.of("type", "number")));
    PropertySetEntryEntity entry = new PropertySetEntryEntity();
    entry.setValues(Map.of("rating", 88));
    String entryName = "briareus:post:1";

    BlogPropertySet result = mapper.map(schema, entry, entryName);

    assertThat(result.getSchema().getName()).isEqualTo("review-attributes");
    assertThat(result.getValues()).isEqualTo(Map.of("rating", 88));
    assertThat(result.getEntryName()).isEqualTo(entryName);
  }

  @Test
  void map_shouldHandleNullEntry() {
    PropertySetSchemaEntity schema = new PropertySetSchemaEntity();
    schema.setId(UUID.randomUUID());
    schema.setOwnerKey("Blog");
    schema.setName("review-attributes");
    schema.setProperties(Map.of());

    BlogPropertySet result = mapper.map(schema, null, "briareus:post:1");

    assertThat(result.getValues()).isNull();
  }
}

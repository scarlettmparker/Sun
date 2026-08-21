package com.sun.apollo.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.apollo.codegen.types.Stem;
import com.sun.apollo.model.StemEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StemMapperTest {

  private final StemMapper mapper = new StemMapper();

  @Test
  void map_shouldMapAllFields() {
    StemEntity entity = new StemEntity();
    entity.setId(UUID.randomUUID());
    entity.setFilePath("drums.mp3");
    entity.setName("Drums");

    Stem result = mapper.map(entity);

    assertThat(result.getPath()).isEqualTo("drums.mp3");
    assertThat(result.getName()).isEqualTo("Drums");
  }
}

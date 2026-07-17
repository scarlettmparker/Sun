package com.sun.hades.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.model.ReaderPositionEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReaderPositionMapperTest {

  private final ReaderPositionMapper mapper = new ReaderPositionMapper();

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    UUID textId = UUID.randomUUID();
    ReaderPositionEntity entity = new ReaderPositionEntity();
    entity.setId(id);
    entity.setTextId(textId);
    entity.setStartOffset(12);
    entity.setEndOffset(34);

    var result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getTextId()).isEqualTo(textId.toString());
    assertThat(result.getStartOffset()).isEqualTo(12);
    assertThat(result.getEndOffset()).isEqualTo(34);
  }
}

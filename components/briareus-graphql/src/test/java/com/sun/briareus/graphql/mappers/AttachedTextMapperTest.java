package com.sun.briareus.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.briareus.codegen.types.AttachedText;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderTextStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttachedTextMapperTest {

  private final AttachedTextMapper mapper = new AttachedTextMapper();

  @Test
  void map_shouldMapAllFields() {
    ReaderTextEntity entity = new ReaderTextEntity();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setTitle("title");
    entity.setLanguage("en");
    entity.setLevel(CefrLevel.A1);
    entity.setStatus(ReaderTextStatus.ACTIVE);

    AttachedText result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getTitle()).isEqualTo("title");
    assertThat(result.getLanguage()).isEqualTo("en");
    assertThat(result.getLevel()).isEqualTo("A1");
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
  }
}

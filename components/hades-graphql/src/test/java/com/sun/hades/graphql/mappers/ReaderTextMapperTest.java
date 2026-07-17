package com.sun.hades.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderTextStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReaderTextMapperTest {

  private final ReaderTextMapper mapper = new ReaderTextMapper();

  @Test
  void map_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 11, 0);
    UUID sourceId = UUID.randomUUID();

    ReaderTextEntity entity = new ReaderTextEntity();
    entity.setId(UUID.randomUUID());
    entity.setTitle("Title");
    entity.setContent("content");
    entity.setLanguage("fr");
    entity.setLevel(CefrLevel.A1);
    entity.setSourceId(sourceId);
    entity.setStatus(ReaderTextStatus.ACTIVE);
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(updatedAt);

    var result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(entity.getId().toString());
    assertThat(result.getTitle()).isEqualTo("Title");
    assertThat(result.getContent()).isEqualTo("content");
    assertThat(result.getLanguage()).isEqualTo("fr");
    assertThat(result.getLevel()).isEqualTo(CefrLevel.A1);
    assertThat(result.getSourceId()).isEqualTo(sourceId.toString());
    assertThat(result.getStatus()).isEqualTo(ReaderTextStatus.ACTIVE);
  }

  @Test
  void mapInput_shouldMapAllFields() {
    ReaderTextInput input = ReaderTextInput.newBuilder()
        .title("Title").content("content").language("fr")
        .level(CefrLevel.B2).build();

    ReaderTextEntity result = mapper.mapInput(input);

    assertThat(result.getTitle()).isEqualTo("Title");
    assertThat(result.getContent()).isEqualTo("content");
    assertThat(result.getLanguage()).isEqualTo("fr");
    assertThat(result.getLevel()).isEqualTo(CefrLevel.B2);
  }
}

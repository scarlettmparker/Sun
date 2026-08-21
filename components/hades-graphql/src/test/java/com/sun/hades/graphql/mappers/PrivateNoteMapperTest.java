package com.sun.hades.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.codegen.types.PrivateNoteVisibility;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.codegen.types.RemoteUserType;
import com.sun.hades.model.PrivateNoteEntity;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrivateNoteMapperTest {

  private final PrivateNoteMapper mapper = new PrivateNoteMapper();

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    UUID textId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 11, 0);
    PrivateNoteEntity entity = new PrivateNoteEntity();
    entity.setId(id);
    entity.setOwnerId(ownerId);
    entity.setTextId(textId);
    entity.setStartOffset(5);
    entity.setEndOffset(15);
    entity.setBody("note body");
    entity.setVisibility(com.sun.hades.model.enums.PrivateNoteVisibility.PRIVATE);
    entity.setRemoteObject(List.of("private_note", "hades:text:" + textId));
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(updatedAt);
    RemoteUser author = RemoteUser.newBuilder()
        .type(RemoteUserType.DISCORD).id("123").build();

    var result = mapper.map(entity, author);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getTextId()).isEqualTo(textId.toString());
    assertThat(result.getStartOffset()).isEqualTo(5);
    assertThat(result.getEndOffset()).isEqualTo(15);
    assertThat(result.getBody()).isEqualTo("note body");
    assertThat(result.getVisibility()).isEqualTo(PrivateNoteVisibility.PRIVATE);
    assertThat(result.getRemoteObject()).containsExactly("private_note", "hades:text:" + textId);
    assertThat(result.getAuthor()).isEqualTo(author);
    assertThat(result.getCreatedAt()).isEqualTo(createdAt.atOffset(ZoneOffset.UTC));
    assertThat(result.getUpdatedAt()).isEqualTo(updatedAt.atOffset(ZoneOffset.UTC));
  }

  @Test
  void map_shouldHandleNullAuthorAndNullTimestamps() {
    UUID id = UUID.randomUUID();
    UUID textId = UUID.randomUUID();
    PrivateNoteEntity entity = new PrivateNoteEntity();
    entity.setId(id);
    entity.setOwnerId(UUID.randomUUID());
    entity.setTextId(textId);
    entity.setStartOffset(0);
    entity.setEndOffset(10);
    entity.setBody("body");
    entity.setVisibility(com.sun.hades.model.enums.PrivateNoteVisibility.SHARED);
    entity.setCreatedAt(null);
    entity.setLastUpdatedAt(null);

    var result = mapper.map(entity, null);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getBody()).isEqualTo("body");
    assertThat(result.getCreatedAt()).isNull();
    assertThat(result.getUpdatedAt()).isNull();
    assertThat(result.getAuthor()).isNull();
    assertThat(result.getVisibility()).isEqualTo(PrivateNoteVisibility.SHARED);
  }
}

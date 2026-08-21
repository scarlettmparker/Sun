package com.sun.icarus.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.icarus.codegen.types.ForumThread;
import com.sun.icarus.model.ForumThreadEntity;
import com.sun.icarus.model.enums.ThreadStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForumThreadMapperTest {

  private final ForumThreadMapper mapper = new ForumThreadMapper();

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.of(2024, 2, 1, 9, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 2, 10, 12, 0);

    ForumThreadEntity entity = new ForumThreadEntity();
    entity.setId(id);
    entity.setTitle("My Thread");
    entity.setStatus(ThreadStatus.ACTIVE);
    entity.setRemoteObject(List.of("hades:annotation:abc"));
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(updatedAt);

    ForumThread result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getTitle()).isEqualTo("My Thread");
    assertThat(result.getStatus()).isEqualTo(ThreadStatus.ACTIVE);
    assertThat(result.getRemoteObject()).containsExactly("hades:annotation:abc");
    assertThat(result.getCreatedAt()).isNotNull();
    assertThat(result.getUpdatedAt()).isNotNull();
  }

  @Test
  void map_withNullTimestamps_shouldMapNulls() {
    UUID id = UUID.randomUUID();
    ForumThreadEntity entity = new ForumThreadEntity();
    entity.setId(id);
    entity.setTitle("No dates");
    entity.setStatus(ThreadStatus.LOCKED);
    entity.setRemoteObject(List.of());
    entity.setCreatedAt(null);
    entity.setLastUpdatedAt(null);

    ForumThread result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getStatus()).isEqualTo(ThreadStatus.LOCKED);
    assertThat(result.getCreatedAt()).isNull();
    assertThat(result.getUpdatedAt()).isNull();
  }

}

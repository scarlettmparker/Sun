package com.sun.echo.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.echo.codegen.types.ChecklistEntry;
import com.sun.echo.codegen.types.ChecklistEntryInput;
import com.sun.echo.model.ChecklistEntryEntity;
import com.sun.echo.model.enums.ChecklistStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistEntryMapperTest {

  private final ChecklistEntryMapper mapper = new ChecklistEntryMapper();

  @Test
  void map_entityToGraphQL() {
    UUID id = UUID.randomUUID();
    LocalDateTime dueAt = LocalDateTime.now();
    LocalDateTime completedAt = LocalDateTime.now();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();

    ChecklistEntryEntity entity = new ChecklistEntryEntity();
    entity.setId(id);
    entity.setName("Weekly shop");
    entity.setDueAt(dueAt);
    entity.setCompletedAt(completedAt);
    entity.setStatus(ChecklistStatus.ACTIVE);
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(updatedAt);

    ChecklistEntry result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getName()).isEqualTo("Weekly shop");
    assertThat(result.getDueAt()).isEqualTo(dueAt.atOffset(ZoneOffset.UTC));
    assertThat(result.getCompletedAt()).isEqualTo(completedAt.atOffset(ZoneOffset.UTC));
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
    assertThat(result.getCreatedAt()).isEqualTo(createdAt.atOffset(ZoneOffset.UTC));
    assertThat(result.getUpdatedAt()).isEqualTo(updatedAt.atOffset(ZoneOffset.UTC));
  }

  @Test
  void map_inputMergesOntoEntity() {
    OffsetDateTime dueAt = OffsetDateTime.now(ZoneOffset.UTC);
    ChecklistEntryInput input = ChecklistEntryInput.newBuilder()
        .name("Updated entry")
        .dueAt(dueAt)
        .status("ARCHIVED")
        .build();
    ChecklistEntryEntity entity = new ChecklistEntryEntity();

    mapper.map(input, entity);

    assertThat(entity.getName()).isEqualTo("Updated entry");
    assertThat(entity.getDueAt()).isEqualTo(dueAt.toLocalDateTime());
    assertThat(entity.getStatus()).isEqualTo(ChecklistStatus.ARCHIVED);
  }
}

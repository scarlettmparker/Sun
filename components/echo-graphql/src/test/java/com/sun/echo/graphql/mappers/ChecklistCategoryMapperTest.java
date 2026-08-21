package com.sun.echo.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.echo.codegen.types.ChecklistCategory;
import com.sun.echo.codegen.types.ChecklistCategoryInput;
import com.sun.echo.model.ChecklistCategoryEntity;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistCategoryMapperTest {

  private final ChecklistCategoryMapper mapper = new ChecklistCategoryMapper();

  @Test
  void map_entityToGraphQL() {
    UUID id = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();

    ChecklistCategoryEntity entity = new ChecklistCategoryEntity();
    entity.setId(id);
    entity.setName("Travel");
    entity.setDescription("Items for trips");
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(updatedAt);

    ChecklistCategory result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getName()).isEqualTo("Travel");
    assertThat(result.getDescription()).isEqualTo("Items for trips");
    assertThat(result.getCreatedAt()).isEqualTo(createdAt.atOffset(ZoneOffset.UTC));
    assertThat(result.getUpdatedAt()).isEqualTo(updatedAt.atOffset(ZoneOffset.UTC));
  }

  @Test
  void map_inputMergesOntoEntity() {
    ChecklistCategoryInput input = ChecklistCategoryInput.newBuilder()
        .name("Updated")
        .description("new desc")
        .build();
    ChecklistCategoryEntity entity = new ChecklistCategoryEntity();

    mapper.map(input, entity);

    assertThat(entity.getName()).isEqualTo("Updated");
    assertThat(entity.getDescription()).isEqualTo("new desc");
  }
}

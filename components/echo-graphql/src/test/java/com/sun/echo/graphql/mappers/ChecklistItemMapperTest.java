package com.sun.echo.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.echo.codegen.types.ChecklistItem;
import com.sun.echo.codegen.types.ChecklistItemInput;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.model.enums.LifecycleStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistItemMapperTest {

  private final ChecklistItemMapper mapper = new ChecklistItemMapper();

  @Test
  void map_entityToGraphQL() {
    UUID id = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();

    ChecklistItemEntity entity = new ChecklistItemEntity();
    entity.setId(id);
    entity.setName("Buy milk");
    entity.setDescription("2 liters");
    entity.setCategoryId(categoryId);
    entity.setLifecycleStatus(LifecycleStatus.ACTIVE);
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(updatedAt);

    ChecklistItem result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getName()).isEqualTo("Buy milk");
    assertThat(result.getDescription()).isEqualTo("2 liters");
    assertThat(result.getCategoryId()).isEqualTo(categoryId.toString());
    assertThat(result.getLifecycleStatus()).isEqualTo("ACTIVE");
    assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void map_inputMergesOntoEntity() {
    ChecklistItemInput input = ChecklistItemInput.newBuilder()
        .name("Updated")
        .description("desc")
        .categoryId(UUID.randomUUID().toString())
        .lifecycleStatus("RETIRED")
        .build();
    ChecklistItemEntity entity = new ChecklistItemEntity();

    mapper.map(input, entity);

    assertThat(entity.getName()).isEqualTo("Updated");
    assertThat(entity.getDescription()).isEqualTo("desc");
    assertThat(entity.getLifecycleStatus()).isEqualTo(LifecycleStatus.RETIRED);
  }
}

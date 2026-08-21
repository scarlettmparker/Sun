package com.sun.echo.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.echo.codegen.types.ChecklistTemplate;
import com.sun.echo.codegen.types.ChecklistTemplateInput;
import com.sun.echo.model.ChecklistTemplateEntity;
import com.sun.echo.model.enums.ChecklistStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistTemplateMapperTest {

  private final ChecklistTemplateMapper mapper = new ChecklistTemplateMapper();

  @Test
  void map_entityToGraphQL() {
    UUID id = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();

    ChecklistTemplateEntity entity = new ChecklistTemplateEntity();
    entity.setId(id);
    entity.setName("Morning routine");
    entity.setDescription("Daily tasks");
    entity.setStatus(ChecklistStatus.ACTIVE);
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(updatedAt);

    ChecklistTemplate result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getName()).isEqualTo("Morning routine");
    assertThat(result.getDescription()).isEqualTo("Daily tasks");
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
    assertThat(result.getCreatedAt()).isEqualTo(createdAt.atOffset(ZoneOffset.UTC));
    assertThat(result.getUpdatedAt()).isEqualTo(updatedAt.atOffset(ZoneOffset.UTC));
  }

  @Test
  void map_inputMergesOntoEntity() {
    ChecklistTemplateInput input = ChecklistTemplateInput.newBuilder()
        .name("Updated template")
        .description("new desc")
        .status("ARCHIVED")
        .build();
    ChecklistTemplateEntity entity = new ChecklistTemplateEntity();

    mapper.map(input, entity);

    assertThat(entity.getName()).isEqualTo("Updated template");
    assertThat(entity.getDescription()).isEqualTo("new desc");
    assertThat(entity.getStatus()).isEqualTo(ChecklistStatus.ARCHIVED);
  }
}

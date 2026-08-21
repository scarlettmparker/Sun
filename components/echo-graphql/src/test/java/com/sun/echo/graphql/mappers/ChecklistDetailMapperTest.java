package com.sun.echo.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.model.ChecklistEntryDetailEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistDetailMapperTest {

  private final ChecklistDetailMapper mapper = new ChecklistDetailMapper();

  @Test
  void map_entityToGraphQL() {
    UUID ownerId = UUID.randomUUID();
    List<String> remoteObjects = List.of("obj-1", "obj-2");

    ChecklistEntryDetailEntity entity = new ChecklistEntryDetailEntity();
    entity.setOwnerId(ownerId);
    entity.setDescription("Detail notes");
    entity.setRemoteObject(remoteObjects);

    ChecklistDetail result = mapper.map(entity);

    assertThat(result.getOwnerId()).isEqualTo(ownerId.toString());
    assertThat(result.getDescription()).isEqualTo("Detail notes");
    assertThat(result.getRemoteObject()).isEqualTo(remoteObjects);
  }

  @Test
  void map_entityWithNullRemoteObjects() {
    UUID ownerId = UUID.randomUUID();

    ChecklistEntryDetailEntity entity = new ChecklistEntryDetailEntity();
    entity.setOwnerId(ownerId);
    entity.setDescription("No remotes");
    entity.setRemoteObject(null);

    ChecklistDetail result = mapper.map(entity);

    assertThat(result.getOwnerId()).isEqualTo(ownerId.toString());
    assertThat(result.getDescription()).isEqualTo("No remotes");
    assertThat(result.getRemoteObject()).isNull();
  }
}

package com.sun.fates.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.fates.codegen.types.Person;
import com.sun.fates.codegen.types.PersonInput;
import com.sun.fates.model.PersonEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonMapperTest {

  private final PersonMapper mapper = new PersonMapper();

  @Test
  void map_entityToGraphQL() {
    UUID id = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();

    PersonEntity entity = new PersonEntity();
    entity.setId(id);
    entity.setFirstName("Jane");
    entity.setLastName("Doe");
    entity.setDisplayName("Jane D");
    entity.setTitle("Engineer");
    entity.setEmail("jane@example.com");
    entity.setPhone("1234567890");
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(updatedAt);

    Person result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getFirstName()).isEqualTo("Jane");
    assertThat(result.getLastName()).isEqualTo("Doe");
    assertThat(result.getDisplayName()).isEqualTo("Jane D");
    assertThat(result.getTitle()).isEqualTo("Engineer");
    assertThat(result.getEmail()).isEqualTo("jane@example.com");
    assertThat(result.getPhone()).isEqualTo("1234567890");
    assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void map_inputMergesOntoEntity() {
    PersonInput input = PersonInput.newBuilder()
        .firstName("Updated")
        .lastName("Name")
        .email("new@example.com")
        .placeId(UUID.randomUUID().toString())
        .build();
    PersonEntity entity = new PersonEntity();

    mapper.map(input, entity);

    assertThat(entity.getFirstName()).isEqualTo("Updated");
    assertThat(entity.getLastName()).isEqualTo("Name");
    assertThat(entity.getEmail()).isEqualTo("new@example.com");
  }

  @Test
  void map_inputSkipsNullFields() {
    PersonInput input = PersonInput.newBuilder()
        .firstName("OnlyName")
        .build();
    PersonEntity entity = new PersonEntity();
    entity.setLastName("KeepThis");
    entity.setEmail("keep@example.com");

    mapper.map(input, entity);

    assertThat(entity.getFirstName()).isEqualTo("OnlyName");
    assertThat(entity.getLastName()).isEqualTo("KeepThis");
    assertThat(entity.getEmail()).isEqualTo("keep@example.com");
  }
}

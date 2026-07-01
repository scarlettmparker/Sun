package com.sun.fates.graphql.mappers;

import com.sun.fates.codegen.types.Person;
import com.sun.fates.codegen.types.PersonInput;
import com.sun.fates.model.PersonEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting person entities to GraphQL types and back.
 */
@Component
public class PersonMapper {

  /**
   * Maps a person entity to the GraphQL Person type.
   *
   * @param entity the person entity
   * @return the GraphQL Person
   */
  public Person map(PersonEntity entity) {
    return Person.newBuilder()
        .id(entity.getId().toString())
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .displayName(entity.getDisplayName())
        .title(entity.getTitle())
        .email(entity.getEmail())
        .phone(entity.getPhone())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }

  /**
   * Applies the input fields to the entity, for create or update.
   *
   * @param input the person input
   * @param entity the person entity to update
   */
  public void map(PersonInput input, PersonEntity entity) {
    if (input.getFirstName() != null) {
      entity.setFirstName(input.getFirstName());
    }
    if (input.getLastName() != null) {
      entity.setLastName(input.getLastName());
    }
    if (input.getDisplayName() != null) {
      entity.setDisplayName(input.getDisplayName());
    }
    if (input.getTitle() != null) {
      entity.setTitle(input.getTitle());
    }
    if (input.getEmail() != null) {
      entity.setEmail(input.getEmail());
    }
    if (input.getPhone() != null) {
      entity.setPhone(input.getPhone());
    }
    if (input.getPlaceId() != null) {
      entity.setPlaceId(UUID.fromString(input.getPlaceId()));
    }
  }
}

package com.sun.echo.graphql.mappers;

import com.sun.echo.codegen.types.ChecklistCategory;
import com.sun.echo.codegen.types.ChecklistCategoryInput;
import com.sun.echo.model.ChecklistCategoryEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting checklist category entities to GraphQL types and back.
 */
@Component
public class ChecklistCategoryMapper {

  /**
   * Maps a checklist category entity to the GraphQL ChecklistCategory type.
   *
   * @param entity the checklist category entity
   * @return the GraphQL ChecklistCategory
   */
  public ChecklistCategory map(ChecklistCategoryEntity entity) {
    return ChecklistCategory.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName())
        .description(entity.getDescription())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }

  /**
   * Applies the input fields to the entity, for create or update.
   *
   * @param input the checklist category input
   * @param entity the checklist category entity to update
   */
  public void map(ChecklistCategoryInput input, ChecklistCategoryEntity entity) {
    if (input.getName() != null) {
      entity.setName(input.getName());
    }
    if (input.getDescription() != null) {
      entity.setDescription(input.getDescription());
    }
  }
}

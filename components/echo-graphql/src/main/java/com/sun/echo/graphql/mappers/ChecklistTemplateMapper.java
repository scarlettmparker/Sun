package com.sun.echo.graphql.mappers;

import com.sun.echo.codegen.types.ChecklistTemplate;
import com.sun.echo.codegen.types.ChecklistTemplateInput;
import com.sun.echo.model.ChecklistTemplateEntity;
import com.sun.echo.model.enums.ChecklistStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting checklist template entities to GraphQL types and back.
 */
@Component
public class ChecklistTemplateMapper {

  /**
   * Maps a checklist template entity to the GraphQL ChecklistTemplate type.
   *
   * @param entity the checklist template entity
   * @return the GraphQL ChecklistTemplate
   */
  public ChecklistTemplate map(ChecklistTemplateEntity entity) {
    return ChecklistTemplate.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName())
        .description(entity.getDescription())
        .status(entity.getStatus() != null ? entity.getStatus().name() : null)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }

  /**
   * Applies the input fields to the entity, for create or update.
   *
   * @param input the checklist template input
   * @param entity the checklist template entity to update
   */
  public void map(ChecklistTemplateInput input, ChecklistTemplateEntity entity) {
    if (input.getName() != null) {
      entity.setName(input.getName());
    }
    if (input.getDescription() != null) {
      entity.setDescription(input.getDescription());
    }
    if (input.getStatus() != null) {
      entity.setStatus(ChecklistStatus.valueOf(input.getStatus()));
    }
  }
}

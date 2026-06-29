package com.sun.echo.graphql.mappers;

import com.sun.echo.codegen.types.ChecklistEntry;
import com.sun.echo.codegen.types.ChecklistEntryInput;
import com.sun.echo.model.ChecklistEntryEntity;
import com.sun.echo.model.enums.ChecklistStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting checklist entry entities to GraphQL types and back.
 */
@Component
public class ChecklistEntryMapper {

  /**
   * Maps a checklist entry entity to the GraphQL ChecklistEntry type.
   *
   * @param entity the checklist entry entity
   * @return the GraphQL ChecklistEntry
   */
  public ChecklistEntry map(ChecklistEntryEntity entity) {
    return ChecklistEntry.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName())
        .dueAt(entity.getDueAt())
        .completedAt(entity.getCompletedAt())
        .status(entity.getStatus() != null ? entity.getStatus().name() : null)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }

  /**
   * Applies the input fields to the entity, for create or update.
   *
   * @param input the checklist entry input
   * @param entity the checklist entry entity to update
   */
  public void map(ChecklistEntryInput input, ChecklistEntryEntity entity) {
    if (input.getName() != null) {
      entity.setName(input.getName());
    }
    if (input.getDueAt() != null) {
      entity.setDueAt(input.getDueAt());
    }
    if (input.getStatus() != null) {
      entity.setStatus(ChecklistStatus.valueOf(input.getStatus()));
    }
  }
}

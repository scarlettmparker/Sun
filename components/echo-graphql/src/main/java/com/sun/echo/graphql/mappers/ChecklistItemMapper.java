package com.sun.echo.graphql.mappers;

import com.sun.echo.codegen.types.ChecklistItem;
import com.sun.echo.codegen.types.ChecklistItemInput;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.model.enums.LifecycleStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting checklist item entities to GraphQL types and back.
 */
@Component
public class ChecklistItemMapper {

  /**
   * Maps a checklist item entity to the GraphQL ChecklistItem type.
   *
   * @param entity the checklist item entity
   * @return the GraphQL ChecklistItem
   */
  public ChecklistItem map(ChecklistItemEntity entity) {
    return ChecklistItem.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName())
        .description(entity.getDescription())
        .icon(entity.getIcon())
        .categoryId(entity.getCategoryId() != null ? entity.getCategoryId().toString() : null)
        .lifecycleStatus(entity.getLifecycleStatus() != null ? entity.getLifecycleStatus().name() : null)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }

  /**
   * Applies the input fields to the entity, for create or update.
   *
   * @param input the checklist item input
   * @param entity the checklist item entity to update
   */
  public void map(ChecklistItemInput input, ChecklistItemEntity entity) {
    if (input.getName() != null) {
      entity.setName(input.getName());
    }
    if (input.getDescription() != null) {
      entity.setDescription(input.getDescription());
    }
    if (input.getCategoryId() != null) {
      entity.setCategoryId(UUID.fromString(input.getCategoryId()));
    }
    if (input.getLifecycleStatus() != null) {
      entity.setLifecycleStatus(LifecycleStatus.valueOf(input.getLifecycleStatus()));
    }
    if (input.getIcon() != null) {
      entity.setIcon(input.getIcon());
    }
  }
}

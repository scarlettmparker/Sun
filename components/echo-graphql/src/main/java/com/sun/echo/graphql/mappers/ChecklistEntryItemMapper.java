package com.sun.echo.graphql.mappers;

import com.sun.echo.codegen.types.ChecklistEntryItem;
import com.sun.echo.model.ChecklistEntryItemEntity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting checklist entry-item entities to GraphQL types.
 */
@Component
public class ChecklistEntryItemMapper {

  /**
   * Maps an entry-item entity to the GraphQL ChecklistEntryItem type.
   *
   * @param entity the entry-item entity
   * @return the GraphQL ChecklistEntryItem
   */
  public ChecklistEntryItem map(ChecklistEntryItemEntity entity) {
    return ChecklistEntryItem.newBuilder()
        .id(entity.getId().toString())
        .entryId(entity.getEntryId().toString())
        .itemId(entity.getItemId().toString())
        .status(entity.getStatus() != null ? entity.getStatus().name() : null)
        .position(entity.getPosition())
        .build();
  }

  /**
   * Maps a list of entry-item entities to GraphQL ChecklistEntryItem types.
   *
   * @param entities the entry-item entities
   * @return the GraphQL ChecklistEntryItems
   */
  public List<ChecklistEntryItem> map(List<ChecklistEntryItemEntity> entities) {
    return entities.stream().map(this::map).collect(Collectors.toList());
  }
}

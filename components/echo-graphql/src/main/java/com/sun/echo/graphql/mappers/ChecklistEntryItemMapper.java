package com.sun.echo.graphql.mappers;

import com.sun.echo.codegen.types.ChecklistEntryItem;
import com.sun.echo.model.ChecklistEntryItemEntity;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.repository.ChecklistItemRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting checklist entry-item entities to GraphQL types.
 */
@Component
public class ChecklistEntryItemMapper {

  private final ChecklistItemRepository itemRepository;

  public ChecklistEntryItemMapper(ChecklistItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  /**
   * Maps an entry-item entity, resolving the referenced item's name/icon.
   *
   * @param entity the entry-item entity
   * @return the GraphQL ChecklistEntryItem
   */
  public ChecklistEntryItem map(ChecklistEntryItemEntity entity) {
    ChecklistItemEntity item = itemRepository.findById(entity.getItemId()).orElse(null);
    return toBuilder(entity, item).build();
  }

  /**
   * Maps a list of entry-item entities.
   *
   * @param entities the entry-item entities
   * @return the GraphQL ChecklistEntryItems
   */
  public List<ChecklistEntryItem> map(List<ChecklistEntryItemEntity> entities) {
    List<UUID> itemIds = entities.stream()
        .map(ChecklistEntryItemEntity::getItemId)
        .distinct()
        .toList();
    Map<UUID, ChecklistItemEntity> itemsById = itemRepository.findAllById(itemIds).stream()
        .collect(Collectors.toMap(ChecklistItemEntity::getId, item -> item));
    return entities.stream()
        .map(entity -> toBuilder(entity, itemsById.get(entity.getItemId())).build())
        .collect(Collectors.toList());
  }

  private ChecklistEntryItem.Builder toBuilder(
      ChecklistEntryItemEntity entity, ChecklistItemEntity item) {
    return ChecklistEntryItem.newBuilder()
        .id(entity.getId().toString())
        .entryId(entity.getEntryId().toString())
        .itemId(entity.getItemId().toString())
        .name(item != null ? item.getName() : null)
        .icon(item != null ? item.getIcon() : null)
        .status(entity.getStatus() != null ? entity.getStatus().name() : null)
        .position(entity.getPosition());
  }
}
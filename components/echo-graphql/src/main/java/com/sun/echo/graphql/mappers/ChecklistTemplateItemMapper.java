package com.sun.echo.graphql.mappers;

import com.sun.echo.codegen.types.ChecklistTemplateItem;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.model.ChecklistTemplateItemEntity;
import com.sun.echo.repository.ChecklistItemRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting checklist template-item entities to GraphQL types.
 * Resolves each referenced item's name/icon inline so callers need not join.
 */
@Component
public class ChecklistTemplateItemMapper {

  private final ChecklistItemRepository itemRepository;

  public ChecklistTemplateItemMapper(ChecklistItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  /**
   * Maps a template-item entity, resolving the referenced item's name/icon.
   *
   * @param entity the template-item entity
   * @return the GraphQL ChecklistTemplateItem
   */
  public ChecklistTemplateItem map(ChecklistTemplateItemEntity entity) {
    ChecklistItemEntity item = itemRepository.findById(entity.getItemId()).orElse(null);
    return toBuilder(entity, item).build();
  }

  /**
   * Maps a list of template-item entities.
   *
   * @param entities the template-item entities
   * @return the GraphQL ChecklistTemplateItems
   */
  public List<ChecklistTemplateItem> map(List<ChecklistTemplateItemEntity> entities) {
    List<UUID> itemIds = entities.stream()
        .map(ChecklistTemplateItemEntity::getItemId)
        .distinct()
        .toList();
    Map<UUID, ChecklistItemEntity> itemsById = itemRepository.findAllById(itemIds).stream()
        .collect(Collectors.toMap(ChecklistItemEntity::getId, item -> item));
    return entities.stream()
        .map(entity -> toBuilder(entity, itemsById.get(entity.getItemId())).build())
        .collect(Collectors.toList());
  }

  private ChecklistTemplateItem.Builder toBuilder(
      ChecklistTemplateItemEntity entity, ChecklistItemEntity item) {
    return ChecklistTemplateItem.newBuilder()
        .id(entity.getId().toString())
        .templateId(entity.getTemplateId().toString())
        .itemId(entity.getItemId().toString())
        .name(item != null ? item.getName() : null)
        .icon(item != null ? item.getIcon() : null)
        .position(entity.getPosition());
  }
}
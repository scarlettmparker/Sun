package com.sun.echo.graphql.mappers;

import com.sun.echo.codegen.types.ChecklistTemplateItem;
import com.sun.echo.model.ChecklistTemplateItemEntity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting checklist template-item entities to GraphQL types.
 */
@Component
public class ChecklistTemplateItemMapper {

  /**
   * Maps a template-item entity to the GraphQL ChecklistTemplateItem type.
   *
   * @param entity the template-item entity
   * @return the GraphQL ChecklistTemplateItem
   */
  public ChecklistTemplateItem map(ChecklistTemplateItemEntity entity) {
    return ChecklistTemplateItem.newBuilder()
        .id(entity.getId().toString())
        .templateId(entity.getTemplateId().toString())
        .itemId(entity.getItemId().toString())
        .position(entity.getPosition())
        .build();
  }

  /**
   * Maps a list of template-item entities to GraphQL ChecklistTemplateItem types.
   *
   * @param entities the template-item entities
   * @return the GraphQL ChecklistTemplateItems
   */
  public List<ChecklistTemplateItem> map(List<ChecklistTemplateItemEntity> entities) {
    return entities.stream().map(this::map).collect(Collectors.toList());
  }
}

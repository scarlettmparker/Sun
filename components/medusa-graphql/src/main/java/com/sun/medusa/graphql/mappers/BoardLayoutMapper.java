package com.sun.medusa.graphql.mappers;

import com.sun.medusa.codegen.types.BoardLayout;
import com.sun.medusa.codegen.types.BoardLayoutInput;
import com.sun.medusa.codegen.types.BoardPlacement;
import com.sun.medusa.codegen.types.BoardPlacementInput;
import com.sun.medusa.model.BoardLayoutEntity;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BoardLayoutMapper {

  public BoardLayout map(BoardLayoutEntity entity) {
    return BoardLayout.newBuilder()
        .id(entity.getId().toString())
        .slug(entity.getSlug())
        .name(entity.getName())
        .description(entity.getDescription())
        .placements(entity.getPlacements().stream().map(this::mapPlacement).toList())
        .isSystem(entity.isSystem())
        .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC))
        .updatedAt(entity.getLastUpdatedAt() == null ? null : entity.getLastUpdatedAt().atOffset(ZoneOffset.UTC))
        .build();
  }

  public BoardLayoutEntity mapInput(BoardLayoutInput input) {
    BoardLayoutEntity entity = new BoardLayoutEntity();
    if (input.getId() != null && !input.getId().isBlank()) {
      entity.setId(UUID.fromString(input.getId()));
    }
    entity.setSlug(input.getSlug());
    entity.setName(input.getName());
    entity.setDescription(input.getDescription());
    entity.setPlacements(input.getPlacements().stream().map(this::mapPlacementInput).toList());
    entity.setSystem(false);
    return entity;
  }

  /**
   * Maps a stored placement map to a GraphQL board placement.
   *
   * @param value Stored placement value.
   * @return GraphQL board placement.
   */
  private BoardPlacement mapPlacement(Object value) {
    Map<?, ?> placement = (Map<?, ?>) value;
    return BoardPlacement.newBuilder()
        .definitionId(String.valueOf(placement.get("definitionId")))
        .color(((Number) placement.get("color")).intValue())
        .square(((Number) placement.get("square")).intValue())
        .build();
  }

  /**
   * Maps a GraphQL board placement input to storage JSON.
   *
   * @param value GraphQL placement input.
   * @return Stored placement map.
   */
  private Object mapPlacementInput(BoardPlacementInput value) {
    return Map.of(
        "definitionId", value.getDefinitionId(),
        "color", value.getColor(),
        "square", value.getSquare());
  }
}

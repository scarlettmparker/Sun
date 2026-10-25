package com.sun.medusa.graphql.mappers;

import com.sun.medusa.codegen.types.BoardLayout;
import com.sun.medusa.codegen.types.BoardLayoutInput;
import com.sun.medusa.model.BoardLayoutEntity;
import java.time.ZoneOffset;
import java.util.List;
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
        .placements(entity.getPlacements())
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
    entity.setPlacements((List<Object>) input.getPlacements());
    entity.setSystem(false);
    return entity;
  }
}
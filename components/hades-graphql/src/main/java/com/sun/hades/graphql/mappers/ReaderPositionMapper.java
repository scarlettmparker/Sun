package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderPosition;
import com.sun.hades.model.ReaderPositionEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader position entities.
 */
@Component
public class ReaderPositionMapper {

  /**
   * Maps a position entity to the GraphQL ReaderPosition type.
   *
   * @param entity the position entity
   * @return the GraphQL ReaderPosition
   */
  public ReaderPosition map(ReaderPositionEntity entity) {
    return ReaderPosition.newBuilder()
        .id(entity.getId().toString())
        .textId(entity.getTextId().toString())
        .startOffset(entity.getStartOffset())
        .endOffset(entity.getEndOffset())
        .build();
  }
}

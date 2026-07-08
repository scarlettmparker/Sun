package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderPosition;
import com.sun.hades.model.ReaderPositionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader position entities.
 */
@Component
public class ReaderPositionMapper {

  private static final Logger logger = LoggerFactory.getLogger(ReaderPositionMapper.class);

  /**
   * Maps a position entity to the GraphQL ReaderPosition type.
   *
   * @param entity the position entity
   * @return the GraphQL ReaderPosition
   */
  public ReaderPosition map(ReaderPositionEntity entity) {
    logger.debug("Mapping reader position {}-{}", entity.getStartOffset(), entity.getEndOffset());
    ReaderPosition position = ReaderPosition.newBuilder()
        .id(entity.getId().toString())
        .textId(entity.getTextId().toString())
        .startOffset(entity.getStartOffset())
        .endOffset(entity.getEndOffset())
        .build();
    logger.debug("Mapped reader position with id {}", position.getId());
    return position;
  }
}

package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.model.ReaderSourceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader source entities.
 */
@Component
public class ReaderSourceMapper {

  private static final Logger logger = LoggerFactory.getLogger(ReaderSourceMapper.class);

  /**
   * Maps a source entity to the GraphQL ReaderSource type.
   *
   * @param entity the source entity
   * @return the GraphQL ReaderSource
   */
  public ReaderSource map(ReaderSourceEntity entity) {
    logger.debug("Mapping reader source {}", entity.getName());
    ReaderSource source = ReaderSource.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName())
        .url(entity.getUrl())
        .build();
    logger.debug("Mapped reader source {} with id {}", entity.getName(), source.getId());
    return source;
  }
}

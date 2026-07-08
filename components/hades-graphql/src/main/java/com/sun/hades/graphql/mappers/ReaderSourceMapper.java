package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.model.ReaderSourceEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader source entities.
 */
@Component
public class ReaderSourceMapper {

  /**
   * Maps a source entity to the GraphQL ReaderSource type.
   *
   * @param entity the source entity
   * @return the GraphQL ReaderSource
   */
  public ReaderSource map(ReaderSourceEntity entity) {
    return ReaderSource.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName())
        .url(entity.getUrl())
        .build();
  }
}

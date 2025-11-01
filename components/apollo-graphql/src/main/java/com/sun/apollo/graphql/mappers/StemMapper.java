package com.sun.apollo.graphql.mappers;

import com.sun.apollo.codegen.types.Stem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.apollo.model.StemEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting domain Stem entities to GraphQL Stem types.
 */
@Component
public class StemMapper {

  private static final Logger logger = LoggerFactory.getLogger(StemMapper.class);

  /**
   * Maps a domain StemEntity to a GraphQL Stem type.
   *
   * @param stemEntity the domain StemEntity to map
   * @return the mapped GraphQL Stem type
   */
  public Stem map(StemEntity stemEntity) {
    logger.debug("Mapping stem {}", stemEntity.getName());

    Stem stem = Stem.newBuilder()
        .path(stemEntity.getFilePath())
        .name(stemEntity.getName())
        .build();

    logger.debug("Mapped stem {} with path {}", stemEntity.getName(), stem.getPath());

    return stem;
  }
}
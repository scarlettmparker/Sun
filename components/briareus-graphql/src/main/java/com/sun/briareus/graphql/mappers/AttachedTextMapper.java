package com.sun.briareus.graphql.mappers;

import com.sun.briareus.codegen.types.AttachedText;
import com.sun.hades.model.ReaderTextEntity;
import org.springframework.stereotype.Component;

/**
 * Maps reader text entities to attached text.
 */
@Component
public class AttachedTextMapper {

  /**
   * Maps a reader text entity to GraphQL.
   *
   * @param entity the domain entity
   * @return the mapped GraphQL type
   */
  public AttachedText map(ReaderTextEntity entity) {
    return AttachedText.newBuilder()
        .id(entity.getId().toString())
        .title(entity.getTitle())
        .language(entity.getLanguage())
        .level(entity.getLevel().name())
        .status(entity.getStatus().name())
        .build();
  }
}

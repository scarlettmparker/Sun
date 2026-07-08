package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderTextType;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader text entities.
 */
@Component
public class ReaderTextMapper {

  /**
   * Maps a text entity to the GraphQL ReaderText type.
   *
   * @param entity the text entity
   * @return the GraphQL ReaderText
   */
  public ReaderText map(ReaderTextEntity entity) {
    return ReaderText.newBuilder()
        .id(entity.getId().toString())
        .title(entity.getTitle())
        .content(entity.getContent())
        .language(entity.getLanguage())
        .level(entity.getLevel())
        .type(entity.getType())
        .sourceId(entity.getSourceId() != null ? entity.getSourceId().toString() : null)
        .status(entity.getStatus())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }

  /**
   * Applies input fields to a text entity, for create or update.
   *
   * @param input the text input
   * @param entity the text entity to update
   */
  public void map(ReaderTextInput input, ReaderTextEntity entity) {
    entity.setTitle(input.getTitle());
    entity.setContent(input.getContent());
    entity.setLanguage(input.getLanguage());
    if (input.getLevel() != null) {
      entity.setLevel(input.getLevel());
    }
    if (input.getType() != null) {
      entity.setType(input.getType());
    } else {
      entity.setType(ReaderTextType.USER);
    }
    if (input.getSourceId() != null) {
      entity.setSourceId(UUID.fromString(input.getSourceId()));
    }
  }
}

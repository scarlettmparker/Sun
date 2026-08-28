package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.TextVersion;
import com.sun.hades.model.TextVersionEntity;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * Mapper for text version entities.
 */
@Component
public class TextVersionMapper {

  /**
   * Maps a version entity to the GraphQL type.
   *
   * @param entity the entity
   * @return the GraphQL version
   */
  public TextVersion map(TextVersionEntity entity) {
    return TextVersion.newBuilder()
        .id(entity.getId().toString())
        .textId(entity.getTextId().toString())
        .version(entity.getVersion())
        .title(entity.getTitle())
        .content(entity.getContent())
        .level(entity.getLevel())
        .language(entity.getLanguage())
        .editedBy(entity.getEditedBy() == null ? null : entity.getEditedBy().toString())
        .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC))
        .build();
  }
}

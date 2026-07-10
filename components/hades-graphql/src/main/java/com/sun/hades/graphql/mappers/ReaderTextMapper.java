package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.gaia.service.UserContextHolder;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader text entities.
 */
@Component
public class ReaderTextMapper {

  private static final Logger logger = LoggerFactory.getLogger(ReaderTextMapper.class);

  /**
   * Maps a text entity to the GraphQL ReaderText type.
   *
   * @param entity the text entity
   * @return the GraphQL ReaderText
   */
  public ReaderText map(ReaderTextEntity entity) {
    logger.debug("Mapping reader text {}", entity.getTitle());
    ReaderText text = ReaderText.newBuilder()
        .id(entity.getId().toString())
        .title(entity.getTitle())
        .content(entity.getContent())
        .language(entity.getLanguage())
        .level(entity.getLevel())
        .ownerId(entity.getOwnerId() != null ? entity.getOwnerId().toString() : null)
        .sourceId(entity.getSourceId() != null ? entity.getSourceId().toString() : null)
        .status(entity.getStatus())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
    logger.debug("Mapped reader text {} with id {}", entity.getTitle(), text.getId());
    return text;
  }

  /**
   * Maps a GraphQL ReaderTextInput to a new domain ReaderTextEntity.
   *
   * @param input the text input
   * @return the mapped domain ReaderTextEntity
   */
  public ReaderTextEntity mapInput(ReaderTextInput input) {
    logger.debug("Mapping input for reader text {}", input.getTitle());
    ReaderTextEntity entity = new ReaderTextEntity();
    entity.setTitle(input.getTitle());
    entity.setContent(input.getContent());
    entity.setLanguage(input.getLanguage());
    entity.setLevel(input.getLevel() != null ? input.getLevel() : com.sun.hades.model.enums.CefrLevel.A1);
    UUID ownerId =
        input.getOwnerId() != null ? UUID.fromString(input.getOwnerId()) : UserContextHolder.getUserId();
    entity.setOwnerId(ownerId);
    if (input.getSourceId() != null) {
      entity.setSourceId(UUID.fromString(input.getSourceId()));
    }
    return entity;
  }
}

package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.codegen.types.Subtitle;
import com.sun.dionysus.codegen.types.SubtitleSource;
import com.sun.dionysus.model.SubtitleEntity;
import org.springframework.stereotype.Component;

/**
 * Maps SubtitleEntity to the GraphQL Subtitle type.
 */
@Component
public class SubtitleMapper {

  /**
   * Maps a subtitle entity to its GraphQL representation.
   *
   * @param entity the subtitle entity
   * @return the mapped subtitle, or null when the entity is null
   */
  public Subtitle map(SubtitleEntity entity) {
    if (entity == null) {
      return null;
    }

    Subtitle subtitle = Subtitle.newBuilder()
        .bucket(entity.getBucket())
        .keyPath(entity.getKeyPath())
        .subtitleKey(entity.getSubtitleKey())
        .language(entity.getLanguage())
        .source(SubtitleSource.valueOf(entity.getSource().name()))
        .build();

    if (entity.getId() != null) {
      subtitle.setId(entity.getId().toString());
    }
    if (entity.getOpenSubtitlesFileId() != null) {
      subtitle.setOpenSubtitlesFileId(entity.getOpenSubtitlesFileId());
    }
    if (entity.getCreatedAt() != null) {
      subtitle.setCreatedAt(entity.getCreatedAt().toString());
    }
    if (entity.getLastUpdatedAt() != null) {
      subtitle.setLastUpdatedAt(entity.getLastUpdatedAt().toString());
    }
    return subtitle;
  }
}

package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.codegen.types.Subtitle;
import com.sun.dionysus.model.SubtitleEntity;
import com.sun.dionysus.model.enums.SubtitleSource;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SubtitleMapper.
 */
class SubtitleMapperTest {

  private final SubtitleMapper mapper = new SubtitleMapper();

  @Test
  void map_withEntity_mapsAllFields() {
    SubtitleEntity entity = new SubtitleEntity();
    entity.setId(UUID.randomUUID());
    entity.setBucket("b");
    entity.setKeyPath("Movies/Film/film.mkv");
    entity.setSubtitleKey("Movies/Film/film.en.srt");
    entity.setLanguage("en");
    entity.setSource(SubtitleSource.OPENSUBTITLES);
    entity.setOpenSubtitlesFileId(7);
    entity.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4));
    entity.setLastUpdatedAt(LocalDateTime.of(2026, 1, 2, 3, 5));

    Subtitle result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(entity.getId().toString());
    assertThat(result.getBucket()).isEqualTo("b");
    assertThat(result.getKeyPath()).isEqualTo("Movies/Film/film.mkv");
    assertThat(result.getSubtitleKey()).isEqualTo("Movies/Film/film.en.srt");
    assertThat(result.getLanguage()).isEqualTo("en");
    assertThat(result.getSource())
        .isEqualTo(com.sun.dionysus.codegen.types.SubtitleSource.OPENSUBTITLES);
    assertThat(result.getOpenSubtitlesFileId()).isEqualTo(7);
    assertThat(result.getCreatedAt()).isEqualTo("2026-01-02T03:04");
    assertThat(result.getLastUpdatedAt()).isEqualTo("2026-01-02T03:05");
  }

  @Test
  void map_withNull_returnsNull() {
    assertThat(mapper.map(null)).isNull();
  }

  @Test
  void map_withLocalSource_mapsEnum() {
    SubtitleEntity entity = new SubtitleEntity();
    entity.setBucket("b");
    entity.setKeyPath("movie.mkv");
    entity.setSubtitleKey("movie.en.srt");
    entity.setLanguage("en");
    entity.setSource(SubtitleSource.LOCAL);

    Subtitle result = mapper.map(entity);

    assertThat(result.getSource())
        .isEqualTo(com.sun.dionysus.codegen.types.SubtitleSource.LOCAL);
    assertThat(result.getId()).isNull();
    assertThat(result.getOpenSubtitlesFileId()).isNull();
  }
}

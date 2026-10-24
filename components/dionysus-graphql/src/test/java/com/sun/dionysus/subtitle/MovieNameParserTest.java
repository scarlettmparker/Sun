package com.sun.dionysus.subtitle;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class MovieNameParserTest {

  @Test
  void parse_extractsQueryAndYear() {
    MovieNameParser.ParsedMovieName parsed =
        MovieNameParser.parse("Movie.2019.1080p.BluRay.x264.mkv");

    assertThat(parsed.getQuery()).isEqualTo("Movie");
    assertThat(parsed.getYear()).isEqualTo(2019);
    assertThat(parsed.query()).isEqualTo("Movie");
    assertThat(parsed.year()).isEqualTo(2019);
  }

  @Test
  void parse_stripsTranscodedMp4Suffix() {
    MovieNameParser.ParsedMovieName parsed =
        MovieNameParser.parse("Movie.2019.1080p.BluRay.x264.mkv.mp4");

    assertThat(parsed.getQuery()).isEqualTo("Movie");
    assertThat(parsed.getYear()).isEqualTo(2019);
  }

  @Test
  void parse_withoutYear_returnsQueryOnly() {
    MovieNameParser.ParsedMovieName parsed =
        MovieNameParser.parse("Some_Movie_Title.mkv");

    assertThat(parsed.getQuery()).isEqualTo("Some Movie Title");
    assertThat(parsed.getYear()).isNull();
  }

  @Test
  void parse_handlesSpacedFileName() {
    MovieNameParser.ParsedMovieName parsed =
        MovieNameParser.parse("My Movie (2021).mkv");

    assertThat(parsed.getQuery()).isEqualTo("My Movie");
    assertThat(parsed.getYear()).isEqualTo(2021);
  }
}

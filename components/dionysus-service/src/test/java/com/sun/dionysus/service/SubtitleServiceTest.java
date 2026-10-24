package com.sun.dionysus.service;

import com.sun.dionysus.model.SubtitleEntity;
import com.sun.dionysus.model.enums.SubtitleSource;
import com.sun.dionysus.repository.SubtitleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SubtitleService.
 */
@ExtendWith(MockitoExtension.class)
class SubtitleServiceTest {

  @Mock
  private SubtitleRepository repository;

  @InjectMocks
  private SubtitleService service;

  @Test
  void attach_createsWhenNotExists() {
    when(repository.findByBucketAndSubtitleKey("b", "movie.en.srt"))
        .thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

    SubtitleEntity result = service.attach(
        "b", "movie.mkv", "movie.en.srt", "EN", SubtitleSource.OPENSUBTITLES, 42);

    assertThat(result.getBucket()).isEqualTo("b");
    assertThat(result.getKeyPath()).isEqualTo("movie.mkv");
    assertThat(result.getSubtitleKey()).isEqualTo("movie.en.srt");
    assertThat(result.getLanguage()).isEqualTo("en");
    assertThat(result.getSource()).isEqualTo(SubtitleSource.OPENSUBTITLES);
    assertThat(result.getOpenSubtitlesFileId()).isEqualTo(42);
    assertThat(result.getRemoteObject())
        .containsExactly("subtitle", "dionysus:key:b/movie.mkv");
  }

  @Test
  void attach_updatesExistingObject() {
    SubtitleEntity existing = new SubtitleEntity();
    existing.setId(UUID.randomUUID());
    existing.setKeyPath("old.mkv");
    when(repository.findByBucketAndSubtitleKey("b", "movie.en.srt"))
        .thenReturn(Optional.of(existing));
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

    SubtitleEntity result = service.attach(
        "b", "movie.mkv", "movie.en.srt", null, SubtitleSource.LOCAL, null);

    assertThat(result).isSameAs(existing);
    assertThat(result.getKeyPath()).isEqualTo("movie.mkv");
    assertThat(result.getLanguage()).isEqualTo("en");
    assertThat(result.getSource()).isEqualTo(SubtitleSource.LOCAL);
    assertThat(result.getOpenSubtitlesFileId()).isNull();
  }

  @Test
  void listForVideo_delegatesToRepository() {
    SubtitleEntity row = new SubtitleEntity();
    when(repository.findByBucketAndKeyPath("b", "movie.mkv")).thenReturn(List.of(row));

    List<SubtitleEntity> result = service.listForVideo("b", "movie.mkv");

    assertThat(result).containsExactly(row);
  }

  @Test
  void updatePath_movesFolderReferences() {
    SubtitleEntity row = new SubtitleEntity();
    row.setId(UUID.randomUUID());
    row.setBucket("b");
    row.setKeyPath("Movies/Film/film.mkv");
    row.setSubtitleKey("Movies/Film/film.en.srt");
    when(repository.findByPrefix("b", "Movies/Film/")).thenReturn(List.of(row));

    service.updatePath("b", "Movies/Film/", "Movies/Renamed/");

    assertThat(row.getKeyPath()).isEqualTo("Movies/Renamed/film.mkv");
    assertThat(row.getSubtitleKey()).isEqualTo("Movies/Renamed/film.en.srt");
    assertThat(row.getRemoteObject())
        .contains("dionysus:key:b/Movies/Renamed/film.mkv");
    verify(repository).save(row);
  }

  @Test
  void updatePath_movesExactVideoReference() {
    SubtitleEntity row = new SubtitleEntity();
    row.setId(UUID.randomUUID());
    row.setBucket("b");
    row.setKeyPath("a.mkv");
    row.setSubtitleKey("a.en.srt");
    when(repository.findByPrefix("b", "a.mkv/")).thenReturn(List.of());
    when(repository.findByBucketAndKeyPath("b", "a.mkv")).thenReturn(List.of(row));

    service.updatePath("b", "a.mkv", "b.mkv");

    assertThat(row.getKeyPath()).isEqualTo("b.mkv");
    assertThat(row.getSubtitleKey()).isEqualTo("a.en.srt");
    verify(repository).save(row);
  }

  @Test
  void deleteForVideo_delegatesToRepository() {
    service.deleteForVideo("b", "movie.mkv");

    verify(repository).deleteForVideo("b", "movie.mkv");
  }

  @Test
  void deleteUnderPrefix_appendsTrailingSlash() {
    service.deleteUnderPrefix("b", "Movies/Film");

    verify(repository).deleteUnderPrefix("b", "Movies/Film/");
    verify(repository, never()).deleteBySubtitleKey(any(), any());
  }
}

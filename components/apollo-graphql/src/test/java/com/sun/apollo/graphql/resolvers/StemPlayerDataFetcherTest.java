package com.sun.apollo.graphql.resolvers;

import com.sun.apollo.codegen.types.Song;
import com.sun.apollo.codegen.types.Stem;
import com.sun.apollo.graphql.mappers.SongMapper;
import com.sun.apollo.model.SongEntity;
import com.sun.apollo.model.StemEntity;
import com.sun.apollo.service.ApolloService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StemPlayerDataFetcherTest {

  @Mock
  private ApolloService apolloService;

  @Mock
  private SongMapper songMapper;

  @InjectMocks
  private StemPlayerDataFetcher stemPlayerDataFetcher;

  private SongEntity songEntity1;
  private SongEntity songEntity2;
  private Song song1;
  private Song song2;

  @BeforeEach
  void setUp() {
    StemEntity stemEntity1 = new StemEntity();
    stemEntity1.setId(UUID.randomUUID());
    stemEntity1.setFilePath("drums.mp3");
    stemEntity1.setName("Drums");

    songEntity1 = new SongEntity();
    songEntity1.setId(UUID.randomUUID());
    songEntity1.setName("Test Song 1");
    songEntity1.setStems(Arrays.asList(stemEntity1));

    songEntity2 = new SongEntity();
    songEntity2.setId(UUID.randomUUID());
    songEntity2.setName("Test Song 2");
    songEntity2.setStems(Arrays.asList());

    Stem stem1 = Stem.newBuilder()
        .path("drums.mp3")
        .name("Drums")
        .build();

    song1 = Song.newBuilder()
        .id(songEntity1.getId().toString())
        .name("Test Song 1")
        .stems(Arrays.asList(stem1))
        .build();

    song2 = Song.newBuilder()
        .id(songEntity2.getId().toString())
        .name("Test Song 2")
        .stems(Arrays.asList())
        .build();
  }

  @Test
  void list_shouldReturnMappedSongs() {
    List<SongEntity> songEntities = Arrays.asList(songEntity1, songEntity2);
    when(apolloService.listSongs()).thenReturn(songEntities);
    when(songMapper.map(songEntity1)).thenReturn(song1);
    when(songMapper.map(songEntity2)).thenReturn(song2);
    List<Song> result = stemPlayerDataFetcher.list();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Test Song 1");
    assertThat(result.get(0).getStems()).hasSize(1);
    assertThat(result.get(0).getStems().get(0).getName()).isEqualTo("Drums");
    assertThat(result.get(1).getName()).isEqualTo("Test Song 2");
    assertThat(result.get(1).getStems()).isEmpty();
  }

  @Test
  void list_shouldReturnEmptyListWhenNoSongs() {
    when(apolloService.listSongs()).thenReturn(Arrays.asList());
    List<Song> result = stemPlayerDataFetcher.list();
    assertThat(result).isEmpty();
  }

  @Test
  void locate_shouldReturnMappedSong() {
    when(apolloService.locateSong(songEntity1.getId())).thenReturn(Optional.of(songEntity1));
    when(songMapper.map(songEntity1)).thenReturn(song1);
    Song result = stemPlayerDataFetcher.locate(songEntity1.getId().toString());
    assertThat(result.getName()).isEqualTo("Test Song 1");
    assertThat(result.getStems()).hasSize(1);
    assertThat(result.getStems().get(0).getName()).isEqualTo("Drums");
  }

  @Test
  void locate_shouldThrowExceptionWhenSongNotFound() {
    when(apolloService.locateSong(songEntity1.getId())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> stemPlayerDataFetcher.locate(songEntity1.getId().toString()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Song not found with id: " + songEntity1.getId().toString());
  }
}

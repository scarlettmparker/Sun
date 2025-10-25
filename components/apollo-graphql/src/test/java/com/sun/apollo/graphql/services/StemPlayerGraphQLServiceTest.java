package com.sun.apollo.graphql.services;

import com.sun.apollo.graphql.mappers.SongMapper;
import com.sun.apollo.service.ApolloService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sun.apollo.model.StemEntity;
import com.sun.apollo.model.SongEntity;
import com.sun.apollo.codegen.types.Song;
import com.sun.apollo.codegen.types.Stem;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StemPlayerGraphQLServiceTest {

  @Mock
  private ApolloService apolloService;

  @Mock
  private SongMapper songMapper;

  @InjectMocks
  private StemPlayerGraphQLService stemPlayerGraphQLService;

  private SongEntity domainSong1;
  private SongEntity domainSong2;
  private Song graphQLSong1;
  private Song graphQLSong2;

  @BeforeEach
  void setUp() {
    // Setup domain entities
    StemEntity domainStem1 = new StemEntity();
    domainStem1.setId(UUID.randomUUID());
    domainStem1.setFilePath("drums.mp3");
    domainStem1.setName("Drums");

    domainSong1 = new SongEntity();
    domainSong1.setId(UUID.randomUUID());
    domainSong1.setName("Test Song 1");
    domainSong1.setStems(Arrays.asList(domainStem1));

    domainSong2 = new SongEntity();
    domainSong2.setId(UUID.randomUUID());
    domainSong2.setName("Test Song 2");
    domainSong2.setStems(Arrays.asList());

    // Setup GraphQL entities
    Stem graphQLStem1 = Stem.newBuilder()
        .filePath("drums.mp3")
        .name("Drums")
        .build();

    graphQLSong1 = Song.newBuilder()
        .id(domainSong1.getId().toString())
        .name("Test Song 1")
        .stems(Arrays.asList(graphQLStem1))
        .build();

    graphQLSong2 = Song.newBuilder()
        .id(domainSong2.getId().toString())
        .name("Test Song 2")
        .stems(Arrays.asList())
        .build();
  }

  @Test
  void getAllSongs_shouldReturnMappedSongs() {
    // Given
    List<SongEntity> domainSongs = Arrays.asList(domainSong1, domainSong2);
    when(apolloService.findAll()).thenReturn(domainSongs);
    when(songMapper.map(domainSong1)).thenReturn(graphQLSong1);
    when(songMapper.map(domainSong2)).thenReturn(graphQLSong2);

    // When
    List<Song> result = stemPlayerGraphQLService.getAllSongs();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Test Song 1");
    assertThat(result.get(0).getStems()).hasSize(1);
    assertThat(result.get(0).getStems().get(0).getName()).isEqualTo("Drums");
    assertThat(result.get(1).getName()).isEqualTo("Test Song 2");
    assertThat(result.get(1).getStems()).isEmpty();
  }

  @Test
  void getAllSongs_shouldReturnEmptyListWhenNoSongs() {
    // Given
    when(apolloService.findAll()).thenReturn(Arrays.asList());

    // When
    List<Song> result = stemPlayerGraphQLService.getAllSongs();

    // Then
    assertThat(result).isEmpty();
  }
}
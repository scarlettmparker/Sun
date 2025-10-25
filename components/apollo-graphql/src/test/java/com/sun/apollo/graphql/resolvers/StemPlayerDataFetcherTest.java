package com.sun.apollo.graphql.resolvers;

import com.sun.apollo.codegen.types.Song;
import com.sun.apollo.codegen.types.StemPlayerQueries;
import com.sun.apollo.graphql.services.StemPlayerGraphQLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StemPlayerDataFetcherTest {

  @Mock
  private StemPlayerGraphQLService stemPlayerGraphQLService;

  @InjectMocks
  private StemPlayerDataFetcher stemPlayerDataFetcher;

  private List<Song> mockSongs;

  @BeforeEach
  void setUp() {
    Song song1 = Song.newBuilder()
        .id("1")
        .name("Test Song 1")
        .build();

    Song song2 = Song.newBuilder()
        .id("2")
        .name("Test Song 2")
        .build();

    mockSongs = Arrays.asList(song1, song2);
  }

  @Test
  void getStemPlayerQueries_shouldReturnStemPlayerQueriesInstance() {
    // When
    StemPlayerQueries result = stemPlayerDataFetcher.getStemPlayerQueries();

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  void listSongs_shouldReturnSongsFromService() {
    // Given
    when(stemPlayerGraphQLService.getAllSongs()).thenReturn(mockSongs);

    // When
    List<Song> result = stemPlayerDataFetcher.listSongs();

    // Then
    assertThat(result).isEqualTo(mockSongs);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Test Song 1");
    assertThat(result.get(1).getName()).isEqualTo("Test Song 2");
  }
}
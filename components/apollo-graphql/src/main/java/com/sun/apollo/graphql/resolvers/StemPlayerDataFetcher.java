package com.sun.apollo.graphql.resolvers;
import com.sun.base.audit.annotation.Audited;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.apollo.codegen.types.Song;
import com.sun.apollo.codegen.types.StemPlayerQueries;
import com.sun.apollo.graphql.mappers.SongMapper;
import com.sun.apollo.model.SongEntity;
import com.sun.apollo.service.ApolloService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import com.sun.base.audit.annotation.NotAudited;
/**
 * GraphQL data fetcher for Stem Player queries.
 * Handles the resolution of stem player-related GraphQL queries.
 */
@DgsComponent
public class StemPlayerDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(StemPlayerDataFetcher.class);

  private final ApolloService apolloService;
  private final SongMapper songMapper;

  public StemPlayerDataFetcher(ApolloService apolloService, SongMapper songMapper) {
    this.apolloService = apolloService;
    this.songMapper = songMapper;
  }

  /**
   * Provides the stem player queries object.
   *
   * @return a new StemPlayerQueries instance
   */
  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Query", field = "stemPlayerQueries")
  public StemPlayerQueries getStemPlayerQueries() {
    return StemPlayerQueries.newBuilder().build();
  }

  /**
   * Retrieves all songs for the stem player (without stems).
   *
   * @return a list of Song objects
   */
  @Audited(event = "LIST")
  @DgsData(parentType = "StemPlayerQueries", field = "list")
  @PreAuthorize("@permissions.has('graphql.apollo.list')")
  @Transactional
  public List<Song> list() {
    logger.info("Retrieving songs for stem player (without stems)");

    List<SongEntity> songEntities = apolloService.listSongs();
    List<Song> songs = songEntities.stream()
        .map(songEntity -> songMapper.map(songEntity))
        .collect(Collectors.toList());

    logger.info("Retrieved {} songs", songs.size());
    return songs;
  }

  /**
   * Retrieves a specific song with all its stems by ID.
   *
   * @param id the song ID
   * @return the Song object
   */
  @Audited(event = "LOCATE")
  @DgsData(parentType = "StemPlayerQueries", field = "locate")
  @PreAuthorize("@permissions.has('graphql.apollo.locate')")
  @Transactional
  public Song locate(String id) {
    logger.info("Retrieving song by ID: {}", id);

    SongEntity songEntity = apolloService.locateSong(UUID.fromString(id))
        .orElseThrow(() -> new RuntimeException("Song not found with id: " + id));

    Song song = songMapper.map(songEntity);

    logger.info("Retrieved song {} with id {}", songEntity.getName(), song.getId());
    return song;
  }
}

package com.sun.apollo.graphql.services;

import com.sun.apollo.service.ApolloService;
import com.sun.apollo.graphql.mappers.SongMapper;
import com.sun.apollo.codegen.types.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import com.sun.apollo.model.SongEntity;

/**
 * Service for handling GraphQL-specific business logic for the Stem Player.
 * This service acts as an intermediary between the GraphQL layer and the domain services.
 */
@Service
public class StemPlayerGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(StemPlayerGraphQLService.class);

  @Autowired
  private ApolloService apolloService;

  @Autowired
  private SongMapper songMapper;

  /**
   * Retrieves all songs for the stem player (without stems).
   *
   * @return a list of GraphQL Song objects without stems
   */
  public List<Song> list() {
    logger.info("Retrieving songs for stem player (without stems)");

    List<SongEntity> songEntities = apolloService.listSongs();
    List<Song> songs = songEntities.stream()
      .map(songMapper::map)
      .collect(Collectors.toList());

    logger.info("Retrieved {} songs", songs.size());
    return songs;
  }

  /**
   * Retrieves a specific song with all its stems by ID.
   *
   * @param id the song ID as string
   * @return the GraphQL Song object with stems
   */
  public Song locate(String id) {
    logger.info("Retrieving song by ID: {}", id);

    SongEntity songEntity = apolloService.locateSong(java.util.UUID.fromString(id))
      .orElseThrow(() -> new RuntimeException("Song not found with id: " + id));

    Song song = songMapper.map(songEntity);

    logger.info("Retrieved song {} with id {}", songEntity.getName(), song.getId());
    return song;
  }
}
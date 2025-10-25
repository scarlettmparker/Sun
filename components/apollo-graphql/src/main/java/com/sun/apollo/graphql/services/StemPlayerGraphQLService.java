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
   * Retrieves all songs for the stem player.
   *
   * @return a list of GraphQL Song objects
   */
  public List<Song> getAllSongs() {
    logger.info("Retrieving songs for stem player");

    List<SongEntity> domainSongs = apolloService.findAll();
    List<Song> graphQLSongs = domainSongs.stream()
        .map(songMapper::map)
        .collect(Collectors.toList());

    logger.info("Retrieved {} songs", graphQLSongs.size());
    return graphQLSongs;
  }
}
package com.sun.apollo.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.apollo.graphql.services.StemPlayerGraphQLService;
import com.sun.apollo.codegen.types.Song;
import com.sun.apollo.codegen.types.StemPlayerQueries;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * GraphQL data fetcher for Stem Player queries.
 * Handles the resolution of stem player-related GraphQL queries.
 */
@DgsComponent
public class StemPlayerDataFetcher {

  @Autowired
  private StemPlayerGraphQLService stemPlayerGraphQLService;

  /**
   * Provides the stem player queries object.
   *
   * @return a new StemPlayerQueries instance
   */
  @DgsData(parentType = "Query", field = "stemPlayerQueries")
  public StemPlayerQueries getStemPlayerQueries() {
    return StemPlayerQueries.newBuilder().build();
  }

  /**
   * Retrieves all songs for the stem player.
   *
   * @return a list of GraphQL Song objects
   */
  @DgsData(parentType = "StemPlayerQueries", field = "listSongs")
  public List<Song> listSongs() {
    return stemPlayerGraphQLService.getAllSongs();
  }
}
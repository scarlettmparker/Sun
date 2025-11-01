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
   * Retrieves all songs for the stem player (without stems).
   *
   * @return a list of Song objects
   */
  @DgsData(parentType = "StemPlayerQueries", field = "list")
  public List<Song> list() {
    return stemPlayerGraphQLService.list();
  }

  /**
   * Retrieves a specific song with all its stems by ID.
   *
   * @param id the song ID
   * @return the Song object
   */
  @DgsData(parentType = "StemPlayerQueries", field = "locate")
  public Song locate(String id) {
    return stemPlayerGraphQLService.locate(id);
  }
}
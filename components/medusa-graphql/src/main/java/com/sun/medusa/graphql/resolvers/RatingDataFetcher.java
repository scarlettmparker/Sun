package com.sun.medusa.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.audit.annotation.Audited;
import com.sun.medusa.codegen.types.PlayerRating;
import com.sun.medusa.graphql.services.RatingGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for Free Chess player ratings.
 */
@DgsComponent
public class RatingDataFetcher {

  private final RatingGraphQLService ratingGraphQLService;

  public RatingDataFetcher(RatingGraphQLService ratingGraphQLService) {
    this.ratingGraphQLService = ratingGraphQLService;
  }

  /**
   * Loads ratings for a set of accounts.
   *
   * @param ids account ids
   * @return ratings
   */
  @Audited(event = "LIST", entity = "PlayerRating")
  @DgsData(parentType = "MedusaQueries", field = "playerRatings")
  @PreAuthorize("@permissions.has('graphql.medusa.playerRatings')")
  @Transactional(readOnly = true)
  public List<PlayerRating> playerRatings(List<String> ids) {
    return ratingGraphQLService.playerRatings(ids);
  }

  /**
   * Loads the signed-in account rating.
   *
   * @return rating
   */
  @Audited(event = "LOCATE", entity = "PlayerRating")
  @DgsData(parentType = "MedusaQueries", field = "myRating")
  @PreAuthorize("@permissions.has('graphql.medusa.myRating')")
  @Transactional(readOnly = true)
  public PlayerRating myRating() {
    return ratingGraphQLService.myRating();
  }

  /**
   * Lists the top rated players.
   *
   * @param limit maximum number of players
   * @return leaderboard
   */
  @Audited(event = "LIST", entity = "PlayerRating")
  @DgsData(parentType = "MedusaQueries", field = "leaderboard")
  @PreAuthorize("@permissions.has('graphql.medusa.leaderboard')")
  @Transactional(readOnly = true)
  public List<PlayerRating> leaderboard(Integer limit) {
    return ratingGraphQLService.leaderboard(limit);
  }
}

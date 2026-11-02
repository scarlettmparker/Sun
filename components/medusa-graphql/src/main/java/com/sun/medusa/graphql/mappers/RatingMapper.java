package com.sun.medusa.graphql.mappers;

import com.sun.medusa.codegen.types.PlayerRating;
import com.sun.medusa.model.PlayerRatingEntity;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {

  /**
   * Maps a stored rating to its GraphQL shape.
   *
   * @param entity stored rating
   * @param username account username, if known
   * @return GraphQL rating
   */
  public PlayerRating map(PlayerRatingEntity entity, String username) {
    return PlayerRating.newBuilder()
        .accountId(entity.getAccountId().toString())
        .username(username)
        .rating(entity.getRating())
        .deviation(entity.getDeviation())
        .volatility(entity.getVolatility())
        .ratedGames(entity.getRatedGames())
        .peakRating(entity.getPeakRating())
        .provisional(entity.getRatedGames() < 10 || entity.getDeviation() > 110)
        .lastRatedAt(mapDate(entity.getLastRatedAt()))
        .build();
  }

  /**
   * Formats an optional local date.
   *
   * @param value local date
   * @return offset date, or null
   */
  private OffsetDateTime mapDate(LocalDateTime value) {
    return value == null ? null : value.atOffset(ZoneOffset.UTC);
  }
}

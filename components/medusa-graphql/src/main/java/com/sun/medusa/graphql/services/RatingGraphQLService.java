package com.sun.medusa.graphql.services;

import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.medusa.codegen.types.PlayerRating;
import com.sun.medusa.graphql.mappers.RatingMapper;
import com.sun.medusa.model.PlayerRatingEntity;
import com.sun.medusa.service.RatingService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RatingGraphQLService {

  private static final Pattern UUID_PATTERN = Pattern.compile(
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  private final RatingService ratingService;
  private final RatingMapper ratingMapper;
  private final AccountService accountService;

  /**
   * Creates the rating GraphQL service.
   *
   * @param ratingService rating service
   * @param ratingMapper rating mapper
   * @param accountService account service
   */
  public RatingGraphQLService(
      RatingService ratingService,
      RatingMapper ratingMapper,
      AccountService accountService) {
    this.ratingService = ratingService;
    this.ratingMapper = ratingMapper;
    this.accountService = accountService;
  }

  /**
   * Loads ratings for a set of accounts.
   *
   * @param ids account ids
   * @return ratings in the requested order
   */
  public List<PlayerRating> playerRatings(List<String> ids) {
    List<UUID> accountIds = ids.stream()
        .filter(RatingGraphQLService::isUuid)
        .map(UUID::fromString)
        .toList();
    Map<UUID, String> usernames = usernamesFor(accountIds);
    return ratingService.ratingsFor(accountIds).stream()
        .map(rating -> ratingMapper.map(rating, usernames.get(rating.getAccountId())))
        .toList();
  }

  /**
   * Loads the signed-in account rating.
   *
   * @return account rating
   */
  public PlayerRating myRating() {
    UUID caller = UserContextHolder.getUserId();
    if (caller == null) {
      throw new IllegalStateException("An authenticated account is required");
    }
    PlayerRatingEntity rating = ratingService.ratingFor(caller);
    String username = accountService.findById(caller)
        .map(AccountEntity::getUsername)
        .orElse(null);
    return ratingMapper.map(rating, username);
  }

  /**
   * Lists the top rated players.
   *
   * @param limit maximum number of players
   * @return leaderboard
   */
  public List<PlayerRating> leaderboard(Integer limit) {
    List<PlayerRatingEntity> ratings = ratingService.leaderboard(limit == null ? 50 : limit);
    List<UUID> ids = ratings.stream().map(PlayerRatingEntity::getAccountId).toList();
    Map<UUID, String> usernames = usernamesFor(ids);
    return ratings.stream()
        .map(rating -> ratingMapper.map(rating, usernames.get(rating.getAccountId())))
        .toList();
  }

  /**
   * Resolves usernames for a set of accounts.
   *
   * @param ids account ids
   * @return usernames keyed by account id
   */
  private Map<UUID, String> usernamesFor(List<UUID> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    return accountService.findAllByIds(ids).stream()
        .collect(Collectors.toMap(AccountEntity::getId, AccountEntity::getUsername));
  }

  /**
   * Checks whether a string is a UUID.
   *
   * @param value value to check
   * @return true when the value is a UUID
   */
  private static boolean isUuid(String value) {
    return value != null && UUID_PATTERN.matcher(value).matches();
  }
}

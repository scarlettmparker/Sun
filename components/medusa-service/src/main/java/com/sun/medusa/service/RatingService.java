package com.sun.medusa.service;

import com.sun.medusa.model.PlayerRatingEntity;
import com.sun.medusa.model.RatingHistoryEntity;
import com.sun.medusa.rating.Glicko2Calculator;
import com.sun.medusa.rating.RatingChange;
import com.sun.medusa.rating.RatingResult;
import com.sun.medusa.rating.RatingState;
import com.sun.medusa.repository.PlayerRatingRepository;
import com.sun.medusa.repository.RatingHistoryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RatingService {

  public static final int MAX_RATED_PAIR_GAMES_PER_DAY = 5;
  public static final int LEADERBOARD_MIN_GAMES = 10;
  public static final String REASON_PAIR_CAP = "pair-daily-cap";
  public static final String REASON_ABORTED = "game-aborted";
  public static final String REASON_MISSING_ACCOUNT = "missing-account";
  public static final String REASON_DUPLICATE = "duplicate";

  private final PlayerRatingRepository ratingRepository;
  private final RatingHistoryRepository historyRepository;
  private final Glicko2Calculator calculator;

  /**
   * Creates the rating service.
   *
   * @param ratingRepository rating repository
   * @param historyRepository rating history repository
   */
  public RatingService(
      PlayerRatingRepository ratingRepository,
      RatingHistoryRepository historyRepository) {
    this.ratingRepository = ratingRepository;
    this.historyRepository = historyRepository;
    this.calculator = new Glicko2Calculator();
  }

  /**
   * Loads ratings for a set of accounts, filling in defaults for new accounts.
   *
   * @param accountIds account ids
   * @return ratings in the requested order
   */
  @Transactional(readOnly = true)
  public List<PlayerRatingEntity> ratingsFor(Collection<UUID> accountIds) {
    if (accountIds.isEmpty()) {
      return List.of();
    }

    Map<UUID, PlayerRatingEntity> found = new LinkedHashMap<>();
    for (PlayerRatingEntity rating : ratingRepository.findAllByAccountIds(accountIds)) {
      found.put(rating.getAccountId(), rating);
    }

    List<PlayerRatingEntity> ratings = new ArrayList<>(accountIds.size());
    for (UUID accountId : accountIds) {
      ratings.add(found.getOrDefault(accountId, PlayerRatingEntity.defaults(accountId)));
    }
    return ratings;
  }

  /**
   * Loads one account rating, or its default state.
   *
   * @param accountId account id
   * @return rating
   */
  @Transactional(readOnly = true)
  public PlayerRatingEntity ratingFor(UUID accountId) {
    return ratingRepository.findById(accountId)
        .orElseGet(() -> PlayerRatingEntity.defaults(accountId));
  }

  /**
   * Lists the highest rated players with enough rated games.
   *
   * @param limit maximum number of players
   * @return leaderboard
   */
  @Transactional(readOnly = true)
  public List<PlayerRatingEntity> leaderboard(int limit) {
    int size = Math.max(1, Math.min(limit, 100));
    return ratingRepository.findTopRated(
        LEADERBOARD_MIN_GAMES,
        PageRequest.of(0, size));
  }

  /**
   * Applies a game result to both players.
   *
   * @param gameId archived game id
   * @param whiteAccountId white account id
   * @param blackAccountId black account id
   * @param result game result
   * @param playedAt finish time
   * @return rating result
   */
  public RatingResult applyResult(
      UUID gameId,
      UUID whiteAccountId,
      UUID blackAccountId,
      String result,
      LocalDateTime playedAt) {
    List<RatingHistoryEntity> existing = historyRepository.findByGameId(gameId);
    if (!existing.isEmpty()) {
      return new RatingResult(false, REASON_DUPLICATE, existing.stream()
          .map(this::toChange)
          .toList());
    }

    Double whiteScore = scoreForWhite(result);
    if (whiteScore == null) {
      return new RatingResult(false, REASON_ABORTED, List.of());
    }
    if (whiteAccountId == null || blackAccountId == null
        || whiteAccountId.equals(blackAccountId)) {
      return new RatingResult(false, REASON_MISSING_ACCOUNT, List.of());
    }

    LocalDateTime finishedAt = playedAt == null ? LocalDateTime.now() : playedAt;
    long pairGames = historyRepository.countPairGames(
        whiteAccountId,
        blackAccountId,
        finishedAt.minusDays(1));
    if (pairGames >= MAX_RATED_PAIR_GAMES_PER_DAY) {
      return new RatingResult(false, REASON_PAIR_CAP, List.of());
    }

    PlayerRatingEntity white = lockRating(whiteAccountId);
    PlayerRatingEntity black = lockRating(blackAccountId);
    RatingState whiteBefore = toState(white);
    RatingState blackBefore = toState(black);

    RatingState whiteAfter = calculator.update(
        whiteBefore,
        blackBefore,
        whiteScore,
        elapsedDays(white.getLastRatedAt(), finishedAt));
    RatingState blackAfter = calculator.update(
        blackBefore,
        whiteBefore,
        1 - whiteScore,
        elapsedDays(black.getLastRatedAt(), finishedAt));

    RatingChange whiteChange = updateRating(
        white, black.getAccountId(), whiteBefore, whiteAfter,
        resultForAccount(result, true), gameId, finishedAt);
    RatingChange blackChange = updateRating(
        black, white.getAccountId(), blackBefore, blackAfter,
        resultForAccount(result, false), gameId, finishedAt);

    return new RatingResult(true, null, List.of(whiteChange, blackChange));
  }

  /**
   * Locks a rating row, creating a default row when the account is new.
   *
   * @param accountId account id
   * @return locked rating
   */
  private PlayerRatingEntity lockRating(UUID accountId) {
    return ratingRepository.findForUpdate(accountId)
        .orElseGet(() -> ratingRepository.save(PlayerRatingEntity.defaults(accountId)));
  }

  /**
   * Updates one rating and writes its history row.
   *
   * @param entity rating entity
   * @param opponentId opponent account id
   * @param before state before the game
   * @param after state after the game
   * @param result result for this account
   * @param gameId game id
   * @param playedAt finish time
   * @return rating change
   */
  private RatingChange updateRating(
      PlayerRatingEntity entity,
      UUID opponentId,
      RatingState before,
      RatingState after,
      String result,
      UUID gameId,
      LocalDateTime playedAt) {
    double delta = after.rating() - before.rating();
    entity.setRating(after.rating());
    entity.setDeviation(after.deviation());
    entity.setVolatility(after.volatility());
    entity.setRatedGames(entity.getRatedGames() + 1);
    entity.setPeakRating(Math.max(entity.getPeakRating(), after.rating()));
    entity.setLastRatedAt(playedAt);
    entity.setUpdatedAt(LocalDateTime.now());
    ratingRepository.save(entity);

    RatingHistoryEntity history = new RatingHistoryEntity();
    history.setAccountId(entity.getAccountId());
    history.setOpponentAccountId(opponentId);
    history.setGameId(gameId);
    history.setResult(result);
    history.setRatingBefore(before.rating());
    history.setRatingAfter(after.rating());
    history.setDeviationBefore(before.deviation());
    history.setDeviationAfter(after.deviation());
    history.setDelta(delta);
    history.setCreatedAt(playedAt);
    historyRepository.save(history);

    return new RatingChange(
        entity.getAccountId(),
        before.rating(),
        after.rating(),
        before.deviation(),
        after.deviation(),
        delta);
  }

  /**
   * Converts a stored rating to a Glicko-2 state.
   *
   * @param entity rating entity
   * @return rating state
   */
  private RatingState toState(PlayerRatingEntity entity) {
    return new RatingState(entity.getRating(), entity.getDeviation(), entity.getVolatility());
  }

  /**
   * Maps a history row to a rating change.
   *
   * @param history history row
   * @return rating change
   */
  private RatingChange toChange(RatingHistoryEntity history) {
    return new RatingChange(
        history.getAccountId(),
        history.getRatingBefore(),
        history.getRatingAfter(),
        history.getDeviationBefore(),
        history.getDeviationAfter(),
        history.getDelta());
  }

  /**
   * Calculates whole days between two timestamps.
   *
   * @param lastRatedAt previous rating time, if any
   * @param playedAt current rating time
   * @return elapsed days
   */
  private static double elapsedDays(LocalDateTime lastRatedAt, LocalDateTime playedAt) {
    if (lastRatedAt == null) {
      return 0;
    }
    return Math.max(0, Duration.between(lastRatedAt, playedAt).toSeconds() / 86400.0);
  }

  /**
   * Maps a result to the white score.
   *
   * @param result game result
   * @return white score, or null when the game is unrated
   */
  private static Double scoreForWhite(String result) {
    if ("white-checkmate".equals(result) || "white-resignation".equals(result)) {
      return 1.0;
    }
    if ("black-checkmate".equals(result) || "black-resignation".equals(result)) {
      return 0.0;
    }
    if ("stalemate".equals(result) || "draw".equals(result) || "draw-agreement".equals(result)) {
      return 0.5;
    }
    return null;
  }

  /**
   * Maps a game result to the result shown for one account.
   *
   * @param result game result
   * @param white whether the account held white
   * @return win, loss, or draw
   */
  private static String resultForAccount(String result, boolean white) {
    if ("draw".equals(result) || "stalemate".equals(result)
        || "draw-agreement".equals(result)) {
      return "draw";
    }
    boolean whiteWon = "white-checkmate".equals(result) || "white-resignation".equals(result);
    return white == whiteWon ? "win" : "loss";
  }
}

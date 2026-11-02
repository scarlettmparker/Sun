package com.sun.medusa.service;

import com.sun.medusa.model.GameEntity;
import com.sun.medusa.model.GameMoveEntity;
import com.sun.medusa.model.GameMoveId;
import com.sun.medusa.repository.GameMoveRepository;
import com.sun.medusa.repository.GameRecordProjection;
import com.sun.medusa.rating.RatingChange;
import com.sun.medusa.rating.RatingResult;
import com.sun.medusa.repository.GameRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GameService {

  private final GameRepository gameRepository;
  private final GameMoveRepository moveRepository;
  private final RatingService ratingService;

  public GameService(
      GameRepository gameRepository,
      GameMoveRepository moveRepository,
      RatingService ratingService) {
    this.gameRepository = gameRepository;
    this.moveRepository = moveRepository;
    this.ratingService = ratingService;
  }

  /**
   * Finds an archived game by code.
   *
   * @param code the game code
   * @return the game, if found
   */
  public Optional<GameEntity> findByCode(String code) {
    return gameRepository.findByCode(code);
  }

  /**
   * Lists games where the account created the game or held a seat.
   *
   * @param accountId the account id
   * @param pageable the page request
   * @return the games
   */
  public Page<GameEntity> findForAccount(UUID accountId, Pageable pageable) {
    Pageable sorted = pageable.getSort().isSorted()
        ? pageable
        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "finishedAt"));
    return gameRepository.findForAccount(accountId, sorted);
  }

  /**
   * Lists the moves for an archived game.
   *
   * @param gameId the game id
   * @return the moves in ply order
   */
  public List<GameMoveEntity> findMoves(UUID gameId) {
    return moveRepository.findByGameId(gameId);
  }

  /**
   * Counts wins losses and draws for an account.
   *
   * @param accountId the account id
   * @return the game totals
   */
  public GameTotals recordForAccount(UUID accountId) {
    GameRecordProjection record = gameRepository.recordForAccount(accountId);
    if (record == null) {
      return new GameTotals(0, 0, 0);
    }
    return new GameTotals((int) record.getWins(), (int) record.getLosses(),
        (int) record.getDraws());
  }

  /**
   * Archives a finished game and its moves.
   *
   * @param game the game to archive
   * @param moves the ordered moves
   * @return the stored game and rating changes
   */
  public ArchiveResult archive(GameEntity game, List<GameMoveEntity> moves) {
    Optional<GameEntity> existing = gameRepository.findByCode(game.getCode());
    if (existing.isPresent()) {
      GameEntity stored = existing.get();
      return new ArchiveResult(stored, ratingService.changesForGame(stored.getId()));
    }

    game.setArchivedAt(LocalDateTime.now());
    game.setMoveCount(moves.size());
    GameEntity saved = gameRepository.save(game);

    for (GameMoveEntity move : moves) {
      GameMoveId id = move.getId();
      if (id == null) {
        throw new IllegalArgumentException("Move ply is required");
      }
      move.setId(new GameMoveId(saved.getId(), id.getPly()));
    }
    moveRepository.saveAll(moves);

    RatingResult ratingResult = new RatingResult(false, null, List.of());
    if (game.isRated()) {
      ratingResult = ratingService.applyResult(
          saved.getId(),
          saved.getWhiteAccountId(),
          saved.getBlackAccountId(),
          saved.getResult(),
          saved.getFinishedAt());
      applyRatingResult(saved, ratingResult);
      saved = gameRepository.save(saved);
    }

    return new ArchiveResult(saved, ratingResult.changes());
  }

  /**
   * Applies rating snapshots to an archived game.
   *
   * @param game stored game
   * @param result rating result
   */
  private void applyRatingResult(GameEntity game, RatingResult result) {
    if (result.changes().isEmpty()) {
      game.setRated(false);
      game.setUnratedReason(result.unratedReason());
      return;
    }

    for (RatingChange change : result.changes()) {
      if (change.accountId().equals(game.getWhiteAccountId())) {
        game.setWhiteRatingBefore(change.ratingBefore());
        game.setWhiteRatingAfter(change.ratingAfter());
        game.setWhiteRatingDelta(change.delta());
      } else if (change.accountId().equals(game.getBlackAccountId())) {
        game.setBlackRatingBefore(change.ratingBefore());
        game.setBlackRatingAfter(change.ratingAfter());
        game.setBlackRatingDelta(change.delta());
      }
    }
    game.setRated(true);
  }

  /**
   * Archived game and the rating changes produced for it.
   *
   * @param game stored game
   * @param ratingChanges rating changes
   */
  public record ArchiveResult(GameEntity game, List<RatingChange> ratingChanges) {
  }

  /**
   * Win loss and draw totals for an account.
   *
   * @param wins the games won
   * @param losses the games lost
   * @param draws the games drawn
   */
  public record GameTotals(int wins, int losses, int draws) {
    /**
     * Total finished games.
     *
     * @return the played count
     */
    public int played() {
      return wins + losses + draws;
    }
  }
}

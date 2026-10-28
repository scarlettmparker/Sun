package com.sun.medusa.service;

import com.sun.medusa.model.GameEntity;
import com.sun.medusa.model.GameMoveEntity;
import com.sun.medusa.model.GameMoveId;
import com.sun.medusa.repository.GameMoveRepository;
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

  public GameService(GameRepository gameRepository, GameMoveRepository moveRepository) {
    this.gameRepository = gameRepository;
    this.moveRepository = moveRepository;
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
   * Archives a finished game and its moves.
   *
   * @param game the game to archive
   * @param moves the ordered moves
   * @return the stored game
   */
  public GameEntity archive(GameEntity game, List<GameMoveEntity> moves) {
    Optional<GameEntity> existing = gameRepository.findByCode(game.getCode());
    if (existing.isPresent()) {
      return existing.get();
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
    return saved;
  }
}

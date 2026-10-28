package com.sun.medusa.graphql.services;

import com.sun.base.util.PageRequests;
import com.sun.gaia.service.UserContextHolder;
import com.sun.medusa.codegen.types.Game;
import com.sun.medusa.codegen.types.GameArchiveInput;
import com.sun.medusa.codegen.types.GameMove;
import com.sun.medusa.codegen.types.GamePage;
import com.sun.medusa.codegen.types.PageInfo;
import com.sun.medusa.codegen.types.PaginationInput;
import com.sun.medusa.graphql.mappers.GameMapper;
import com.sun.medusa.model.GameEntity;
import com.sun.medusa.model.GameMoveEntity;
import com.sun.medusa.service.GameService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class GameGraphQLService {

  private final GameService gameService;
  private final GameMapper gameMapper;

  public GameGraphQLService(GameService gameService, GameMapper gameMapper) {
    this.gameService = gameService;
    this.gameMapper = gameMapper;
  }

  /**
   * Loads an archived game by code.
   *
   * @param code the game code
   * @return the game, or null
   */
  public Game game(String code) {
    return gameService.findByCode(code)
        .map(entity -> gameMapper.map(entity, List.of()))
        .orElse(null);
  }

  /**
   * Lists the moves for an archived game.
   *
   * @param gameId the game id
   * @return the ordered moves
   */
  public List<GameMove> moves(String gameId) {
    return gameMapper.mapMoveEntities(gameService.findMoves(UUID.fromString(gameId)));
  }

  /**
   * Lists games for the signed-in account.
   *
   * @param pagination the page request
   * @return the game page
   */
  public GamePage myGames(PaginationInput pagination) {
    UUID caller = UserContextHolder.getUserId();
    if (caller == null) {
      throw new IllegalStateException("An authenticated account is required");
    }
    Pageable pageable = toPageable(pagination);
    Page<GameEntity> result = gameService.findForAccount(caller, pageable);
    List<Game> items = result.getContent().stream()
        .map(entity -> gameMapper.map(entity, List.of()))
        .toList();
    return GamePage.newBuilder()
        .items(items)
        .pageInfo(pageInfo(result))
        .build();
  }

  /**
   * Archives a finished game and its moves.
   *
   * @param input the archive input
   * @return the stored game
   */
  public Game archiveGame(GameArchiveInput input) {
    GameEntity entity = gameMapper.mapInput(input);
    List<GameMoveEntity> moves = gameMapper.mapMoves(input);
    GameEntity saved = gameService.archive(entity, moves);
    return gameMapper.map(saved, List.of());
  }

  /**
   * Converts pagination input into a page request.
   *
   * @param pagination the page request
   * @return the pageable
   */
  private Pageable toPageable(PaginationInput pagination) {
    if (pagination == null) {
      return PageRequests.of(0, 20, null, null, "finishedAt", Sort.Direction.DESC);
    }
    return PageRequests.of(pagination.getPage(), pagination.getSize(), pagination.getSortBy(),
        pagination.getSortDir() == null ? null : pagination.getSortDir().name(),
        "finishedAt", Sort.Direction.DESC);
  }

  /**
   * Builds page metadata.
   *
   * @param result the data page
   * @return the page metadata
   */
  private PageInfo pageInfo(Page<?> result) {
    return PageInfo.newBuilder()
        .page(result.getNumber())
        .size(result.getSize())
        .totalPages(result.getTotalPages())
        .totalCount((int) result.getTotalElements())
        .hasNextPage(result.hasNext())
        .hasPreviousPage(result.hasPrevious())
        .build();
  }
}

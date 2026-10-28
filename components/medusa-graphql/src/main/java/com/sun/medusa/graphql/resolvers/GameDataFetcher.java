package com.sun.medusa.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.base.audit.annotation.Audited;
import com.sun.base.audit.annotation.NotAudited;
import com.sun.medusa.codegen.types.Game;
import com.sun.medusa.codegen.types.GameArchiveInput;
import com.sun.medusa.codegen.types.GameMove;
import com.sun.medusa.codegen.types.GamePage;
import com.sun.medusa.codegen.types.GamePlayer;
import com.sun.medusa.codegen.types.GameResponse;
import com.sun.medusa.codegen.types.PaginationInput;
import com.sun.medusa.graphql.services.GameGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for archived game queries and the archive mutation.
 */
@DgsComponent
public class GameDataFetcher {

  private final GameGraphQLService gameGraphQLService;

  public GameDataFetcher(GameGraphQLService gameGraphQLService) {
    this.gameGraphQLService = gameGraphQLService;
  }

  /**
   * Locates an archived game by code.
   *
   * @param code the game code
   * @return the game, or null
   */
  @Audited(event = "LOCATE", entity = "Game")
  @DgsData(parentType = "MedusaQueries", field = "game")
  @PreAuthorize("@permissions.has('graphql.medusa.game')")
  @Transactional(readOnly = true)
  public Game game(String code) {
    return gameGraphQLService.game(code);
  }

  /**
   * Lists archived games for the signed-in account.
   *
   * @param pagination the page request
   * @return the game page
   */
  @Audited(event = "LIST", entity = "Game")
  @DgsData(parentType = "MedusaQueries", field = "myGames")
  @PreAuthorize("@permissions.has('graphql.medusa.myGames')")
  @Transactional(readOnly = true)
  public GamePage myGames(PaginationInput pagination) {
    return gameGraphQLService.myGames(pagination);
  }

  /**
   * Lists players by account id.
   *
   * @param ids the account ids
   * @return the players
   */
  @Audited(event = "LIST", entity = "GamePlayer")
  @DgsData(parentType = "MedusaQueries", field = "players")
  @PreAuthorize("@permissions.has('graphql.medusa.players')")
  @Transactional(readOnly = true)
  public List<GamePlayer> players(List<String> ids) {
    return gameGraphQLService.players(ids);
  }

  /**
   * Lists the moves for an archived game.
   *
   * @param env the data-fetching environment
   * @return the ordered moves
   */
  @NotAudited(reason = "field resolver")
  @DgsData(parentType = "Game", field = "moves")
  @Transactional(readOnly = true)
  public List<GameMove> moves(DgsDataFetchingEnvironment env) {
    Game game = env.getSource();
    if (game == null || game.getId() == null) {
      return List.of();
    }
    return gameGraphQLService.moves(game.getId());
  }

  /**
   * Archives a finished game and its moves.
   *
   * @param input the archive input
   * @return the archive response
   */
  @Audited(event = "SAVE", entity = "Game")
  @DgsData(parentType = "MedusaMutations", field = "archiveGame")
  @PreAuthorize("@permissions.has('graphql.medusa.archiveGame')")
  @Transactional
  public GameResponse archiveGame(GameArchiveInput input) {
    return GameResponse.newBuilder()
        .message("Game archived")
        .game(gameGraphQLService.archiveGame(input))
        .build();
  }
}

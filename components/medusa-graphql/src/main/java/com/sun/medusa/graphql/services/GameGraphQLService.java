package com.sun.medusa.graphql.services;

import com.sun.base.util.PageRequests;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.medusa.codegen.types.Game;
import com.sun.medusa.codegen.types.GameArchiveInput;
import com.sun.medusa.codegen.types.GameMove;
import com.sun.medusa.codegen.types.GamePage;
import com.sun.medusa.codegen.types.GamePlayer;
import com.sun.medusa.codegen.types.GameRecord;
import com.sun.medusa.codegen.types.PageInfo;
import com.sun.medusa.codegen.types.PaginationInput;
import com.sun.medusa.graphql.mappers.GameMapper;
import com.sun.medusa.model.GameEntity;
import com.sun.medusa.model.GameMoveEntity;
import com.sun.medusa.service.GameService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class GameGraphQLService {

  private static final Pattern UUID_PATTERN = Pattern.compile(
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  private final GameService gameService;
  private final GameMapper gameMapper;
  private final AccountService accountService;

  public GameGraphQLService(GameService gameService, GameMapper gameMapper,
      AccountService accountService) {
    this.gameService = gameService;
    this.gameMapper = gameMapper;
    this.accountService = accountService;
  }

  /**
   * Loads an archived game by code.
   *
   * @param code the game code
   * @return the game, or null
   */
  public Game game(String code) {
    return gameService.findByCode(code)
        .map(entity -> gameMapper.map(entity, List.of(), usernamesFor(List.of(entity)),
            UserContextHolder.getUserId()))
        .orElse(null);
  }

  /**
   * Lists players by account id.
   *
   * @param ids the account ids
   * @return the players
   */
  public List<GamePlayer> players(List<String> ids) {
    List<UUID> accountIds = ids.stream()
        .filter(GameGraphQLService::isUuid)
        .map(UUID::fromString)
        .toList();
    Map<String, String> usernames = accountService.findAllByIds(accountIds).stream()
        .collect(Collectors.toMap(
            account -> account.getId().toString(),
            AccountEntity::getUsername));
    return ids.stream()
        .map(id -> GamePlayer.newBuilder()
            .id(id)
            .username(usernames.getOrDefault(id, "Guest"))
            .build())
        .toList();
  }

  /**
   * Checks whether a string is a UUID.
   *
   * @param value the value to check
   * @return true when the value is a UUID
   */
  private static boolean isUuid(String value) {
    return UUID_PATTERN.matcher(value).matches();
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
    Map<UUID, String> usernames = usernamesFor(result.getContent());
    List<Game> items = result.getContent().stream()
        .map(entity -> gameMapper.map(entity, List.of(), usernames, caller))
        .toList();
    return GamePage.newBuilder()
        .items(items)
        .pageInfo(pageInfo(result))
        .record(mapRecord(gameService.recordForAccount(caller)))
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
   * Resolves usernames for the players in a page of games.
   *
   * @param games the games
   * @return usernames keyed by account id
   */
  private Map<UUID, String> usernamesFor(List<GameEntity> games) {
    List<UUID> ids = games.stream()
        .flatMap(game -> Stream.of(game.getWhiteAccountId(), game.getBlackAccountId()))
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    return accountService.findAllByIds(ids).stream()
        .collect(Collectors.toMap(AccountEntity::getId, AccountEntity::getUsername));
  }

  /**
   * Maps account game totals.
   *
   * @param totals the totals
   * @return the GraphQL record
   */
  private GameRecord mapRecord(GameService.GameTotals totals) {
    return GameRecord.newBuilder()
        .wins(totals.wins())
        .losses(totals.losses())
        .draws(totals.draws())
        .played(totals.played())
        .build();
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

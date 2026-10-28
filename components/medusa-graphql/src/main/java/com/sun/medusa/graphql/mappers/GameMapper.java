package com.sun.medusa.graphql.mappers;

import com.sun.medusa.codegen.types.Game;
import com.sun.medusa.codegen.types.GameArchiveInput;
import com.sun.medusa.codegen.types.GameMove;
import com.sun.medusa.codegen.types.GameMoveInput;
import com.sun.medusa.model.GameEntity;
import com.sun.medusa.model.GameMoveEntity;
import com.sun.medusa.model.GameMoveId;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GameMapper {

  /**
   * Maps an archived game and its moves.
   *
   * @param entity the stored game
   * @param moves the ordered moves
   * @return the GraphQL game
   */
  public Game map(GameEntity entity, List<GameMoveEntity> moves) {
    return Game.newBuilder()
        .id(entity.getId().toString())
        .code(entity.getCode())
        .boardLayoutId(mapString(entity.getBoardLayoutId()))
        .boardLayoutName(entity.getBoardLayoutName())
        .status(entity.getStatus())
        .initialState(entity.getInitialState())
        .finalState(entity.getFinalState())
        .creatorAccountId(entity.getCreatorAccountId().toString())
        .whiteAccountId(mapString(entity.getWhiteAccountId()))
        .blackAccountId(mapString(entity.getBlackAccountId()))
        .result(entity.getResult())
        .winnerAccountId(mapString(entity.getWinnerAccountId()))
        .moveCount(entity.getMoveCount())
        .createdAt(mapDate(entity.getGameCreatedAt()))
        .startedAt(mapDate(entity.getStartedAt()))
        .finishedAt(mapDate(entity.getFinishedAt()))
        .archivedAt(mapDate(entity.getArchivedAt()))
        .moves(mapMoveEntities(moves))
        .build();
  }

  /**
   * Maps an archive input to a game entity.
   *
   * @param input the archive input
   * @return the game entity
   */
  public GameEntity mapInput(GameArchiveInput input) {
    GameEntity entity = new GameEntity();
    entity.setCode(input.getCode());
    entity.setBoardLayoutId(mapUuid(input.getBoardLayoutId()));
    entity.setBoardLayoutName(input.getBoardLayoutName());
    entity.setStatus("finished");
    entity.setInitialState(mapState(input.getInitialState()));
    entity.setFinalState(mapState(input.getFinalState()));
    entity.setCreatorAccountId(UUID.fromString(input.getCreatorAccountId()));
    entity.setWhiteAccountId(mapUuid(input.getWhiteAccountId()));
    entity.setBlackAccountId(mapUuid(input.getBlackAccountId()));
    entity.setResult(input.getResult());
    entity.setWinnerAccountId(mapUuid(input.getWinnerAccountId()));
    entity.setGameCreatedAt(mapLocalDate(input.getCreatedAt()));
    entity.setStartedAt(mapLocalDate(input.getStartedAt()));
    entity.setFinishedAt(mapLocalDate(input.getFinishedAt()));
    return entity;
  }

  /**
   * Maps archive move inputs to stored moves.
   *
   * @param input the archive input
   * @return the stored moves
   */
  public List<GameMoveEntity> mapMoves(GameArchiveInput input) {
    return input.getMoves().stream().map(this::mapMoveInput).toList();
  }

  /**
   * Maps stored moves.
   *
   * @param moves the stored moves
   * @return the GraphQL moves
   */
  public List<GameMove> mapMoveEntities(List<GameMoveEntity> moves) {
    return moves.stream().map(this::mapMove).toList();
  }

  /**
   * Maps one stored move.
   *
   * @param entity the stored move
   * @return the GraphQL move
   */
  private GameMove mapMove(GameMoveEntity entity) {
    return GameMove.newBuilder()
        .ply(entity.getId().getPly())
        .move(entity.getMove())
        .playedAt(mapDate(entity.getPlayedAt()))
        .build();
  }

  /**
   * Maps one move input.
   *
   * @param input the move input
   * @return the stored move
   */
  private GameMoveEntity mapMoveInput(GameMoveInput input) {
    return new GameMoveEntity(
        new GameMoveId(null, input.getPly()),
        input.getMove(),
        mapLocalDate(input.getPlayedAt()));
  }

  /**
   * Maps a state object.
   *
   * @param value the state value
   * @return the state map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> mapState(Object value) {
    if (!(value instanceof Map<?, ?>)) {
      throw new IllegalArgumentException("Game state must be an object");
    }
    return (Map<String, Object>) value;
  }

  /**
   * Parses an optional UUID.
   *
   * @param value the UUID string
   * @return the UUID, or null
   */
  private UUID mapUuid(String value) {
    return value == null || value.isBlank() ? null : UUID.fromString(value);
  }

  /**
   * Formats an optional UUID.
   *
   * @param value the UUID
   * @return the UUID string, or null
   */
  private String mapString(UUID value) {
    return value == null ? null : value.toString();
  }

  /**
   * Formats an optional local date.
   *
   * @param value the local date
   * @return the offset date, or null
   */
  private OffsetDateTime mapDate(LocalDateTime value) {
    return value == null ? null : value.atOffset(ZoneOffset.UTC);
  }

  /**
   * Converts an optional offset date to UTC.
   *
   * @param value the offset date
   * @return the local date, or null
   */
  private LocalDateTime mapLocalDate(OffsetDateTime value) {
    return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
  }
}

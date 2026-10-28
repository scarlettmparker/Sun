package com.sun.medusa.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.medusa.codegen.types.Game;
import com.sun.medusa.codegen.types.GameArchiveInput;
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
import org.junit.jupiter.api.Test;

class GameMapperTest {

  private final GameMapper mapper = new GameMapper();

  @Test
  void mapIncludesGameAndMoves() {
    UUID gameId = UUID.randomUUID();
    GameEntity entity = gameEntity(gameId);
    GameMoveEntity move = new GameMoveEntity(
        new GameMoveId(gameId, 0), 1234L, LocalDateTime.of(2026, 1, 1, 12, 1));

    Game game = mapper.map(entity, List.of(move));

    assertThat(game.getId()).isEqualTo(gameId.toString());
    assertThat(game.getCode()).isEqualTo("ARCHIVE");
    assertThat(game.getCreatedAt()).isEqualTo(
        OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC));
    assertThat(game.getMoves()).hasSize(1);
    assertThat(game.getMoves().get(0).getMove()).isEqualTo(1234L);
    assertThat(game.getMoves().get(0).getPly()).isZero();
  }

  @Test
  void mapInputMapsGameFields() {
    GameArchiveInput input = archiveInput();

    GameEntity entity = mapper.mapInput(input);

    assertThat(entity.getCode()).isEqualTo("ARCHIVE");
    assertThat(entity.getStatus()).isEqualTo("finished");
    assertThat(entity.getBoardLayoutId()).hasToString("7f7c5f45-4f4e-4f2a-b9d9-0a3e2d1c3b4a");
    assertThat(entity.getGameCreatedAt()).isEqualTo(
        LocalDateTime.of(2026, 1, 1, 12, 0));
    assertThat(entity.getInitialState()).containsEntry("side", 0);
  }

  @Test
  void mapMovesOrdersInputMoves() {
    GameArchiveInput input = archiveInput();

    List<GameMoveEntity> moves = mapper.mapMoves(input);

    assertThat(moves).hasSize(1);
    assertThat(moves.get(0).getMove()).isEqualTo(1234L);
    assertThat(moves.get(0).getId().getPly()).isZero();
  }

  @Test
  void mapInputRejectsNonObjectState() {
    GameArchiveInput input = archiveInput();
    input.setInitialState(List.of());

    assertThatThrownBy(() -> mapper.mapInput(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Game state must be an object");
  }

  private GameEntity gameEntity(UUID id) {
    GameEntity entity = new GameEntity();
    entity.setId(id);
    entity.setCode("ARCHIVE");
    entity.setBoardLayoutName("Standard");
    entity.setStatus("finished");
    entity.setInitialState(Map.of("side", 0));
    entity.setFinalState(Map.of("side", 1));
    entity.setCreatorAccountId(UUID.randomUUID());
    entity.setMoveCount(1);
    entity.setGameCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
    entity.setStartedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
    entity.setFinishedAt(LocalDateTime.of(2026, 1, 1, 12, 2));
    entity.setArchivedAt(LocalDateTime.of(2026, 1, 1, 12, 3));
    return entity;
  }

  private GameArchiveInput archiveInput() {
    return GameArchiveInput.newBuilder()
        .code("ARCHIVE")
        .boardLayoutId("7f7c5f45-4f4e-4f2a-b9d9-0a3e2d1c3b4a")
        .boardLayoutName("Standard")
        .initialState(Map.of("side", 0))
        .finalState(Map.of("side", 1))
        .creatorAccountId(UUID.randomUUID().toString())
        .createdAt(OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC))
        .finishedAt(OffsetDateTime.of(2026, 1, 1, 12, 2, 0, 0, ZoneOffset.UTC))
        .moves(List.of(GameMoveInput.newBuilder()
            .ply(0)
            .move(1234L)
            .playedAt(OffsetDateTime.of(2026, 1, 1, 12, 1, 0, 0, ZoneOffset.UTC))
            .build()))
        .build();
  }
}

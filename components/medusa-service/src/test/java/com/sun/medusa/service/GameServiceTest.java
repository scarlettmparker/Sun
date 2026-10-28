package com.sun.medusa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.medusa.model.GameEntity;
import com.sun.medusa.model.GameMoveEntity;
import com.sun.medusa.model.GameMoveId;
import com.sun.medusa.repository.GameMoveRepository;
import com.sun.medusa.repository.GameRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

  @Mock private GameRepository gameRepository;
  @Mock private GameMoveRepository moveRepository;

  @InjectMocks private GameService service;

  @Test
  void archiveStoresGameAndMoves() {
    UUID gameId = UUID.randomUUID();
    GameEntity game = gameEntity();
    List<GameMoveEntity> moves = moves();
    when(gameRepository.findByCode("ARCHIVE")).thenReturn(Optional.empty());
    when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> {
      GameEntity saved = invocation.getArgument(0);
      saved.setId(gameId);
      return saved;
    });

    GameEntity result = service.archive(game, moves);

    assertThat(result.getId()).isEqualTo(gameId);
    assertThat(result.getMoveCount()).isEqualTo(2);
    assertThat(result.getArchivedAt()).isNotNull();
    assertThat(moves).extracting(move -> move.getId().getGameId())
        .containsOnly(gameId);
    assertThat(moves).extracting(move -> move.getId().getPly())
        .containsExactly(0, 1);
    verify(moveRepository).saveAll(moves);
  }

  @Test
  void archiveReturnsExistingGame() {
    GameEntity game = gameEntity();
    GameEntity existing = gameEntity();
    existing.setId(UUID.randomUUID());
    when(gameRepository.findByCode("ARCHIVE")).thenReturn(Optional.of(existing));

    GameEntity result = service.archive(game, moves());

    assertThat(result).isSameAs(existing);
    verify(gameRepository, never()).save(any(GameEntity.class));
    verify(moveRepository, never()).saveAll(any());
  }

  @Test
  void archiveRejectsMoveWithoutPly() {
    GameEntity game = gameEntity();
    List<GameMoveEntity> moves = List.of(
        new GameMoveEntity(null, 100, LocalDateTime.now()));
    when(gameRepository.findByCode("ARCHIVE")).thenReturn(Optional.empty());
    when(gameRepository.save(any(GameEntity.class))).thenReturn(game);

    assertThatThrownBy(() -> service.archive(game, moves))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Move ply is required");
  }

  @Test
  void findForAccountUsesNewestFirstByDefault() {
    UUID accountId = UUID.randomUUID();
    when(gameRepository.findForAccount(eq(accountId), any(Pageable.class)))
        .thenReturn(Page.empty());

    service.findForAccount(accountId, PageRequest.of(1, 10));

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(gameRepository).findForAccount(eq(accountId), captor.capture());
    Pageable pageable = captor.getValue();
    assertThat(pageable.getPageNumber()).isEqualTo(1);
    assertThat(pageable.getPageSize()).isEqualTo(10);
    assertThat(pageable.getSort().getOrderFor("finishedAt").getDirection())
        .isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void findMovesDelegatesToRepository() {
    UUID gameId = UUID.randomUUID();
    when(moveRepository.findByGameId(gameId)).thenReturn(moves());

    assertThat(service.findMoves(gameId)).hasSize(2);
    verify(moveRepository).findByGameId(gameId);
  }

  @Test
  void findByCodeDelegatesToRepository() {
    GameEntity game = gameEntity();
    when(gameRepository.findByCode("ARCHIVE")).thenReturn(Optional.of(game));

    assertThat(service.findByCode("ARCHIVE")).contains(game);
  }

  private GameEntity gameEntity() {
    GameEntity game = new GameEntity();
    game.setCode("ARCHIVE");
    game.setBoardLayoutName("Standard");
    game.setStatus("finished");
    game.setCreatorAccountId(UUID.randomUUID());
    game.setWhiteAccountId(UUID.randomUUID());
    game.setBlackAccountId(UUID.randomUUID());
    game.setGameCreatedAt(LocalDateTime.now().minusMinutes(5));
    game.setFinishedAt(LocalDateTime.now());
    return game;
  }

  private List<GameMoveEntity> moves() {
    return List.of(
        new GameMoveEntity(new GameMoveId(null, 0), 100, LocalDateTime.now()),
        new GameMoveEntity(new GameMoveId(null, 1), 200, LocalDateTime.now()));
  }
}

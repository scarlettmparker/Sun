package com.sun.medusa.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.medusa.codegen.types.Game;
import com.sun.medusa.codegen.types.GameArchiveInput;
import com.sun.medusa.codegen.types.GameMove;
import com.sun.medusa.codegen.types.GamePage;
import com.sun.medusa.codegen.types.PaginationInput;
import com.sun.medusa.codegen.types.SortDirection;
import com.sun.medusa.graphql.mappers.GameMapper;
import com.sun.medusa.model.GameEntity;
import com.sun.medusa.model.GameMoveEntity;
import com.sun.medusa.service.GameService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class GameGraphQLServiceTest {

  @Mock private GameService gameService;
  @Mock private GameMapper gameMapper;
  @Mock private AccountService accountService;

  @InjectMocks private GameGraphQLService service;

  @AfterEach
  void clearUser() {
    UserContextHolder.clear();
  }

  @Test
  void gameMapsWithoutMoves() {
    UUID gameId = UUID.randomUUID();
    GameEntity entity = new GameEntity();
    entity.setId(gameId);
    Game game = Game.newBuilder().id(gameId.toString()).build();
    when(gameService.findByCode("ARCHIVE")).thenReturn(Optional.of(entity));
    when(accountService.findAllByIds(List.of())).thenReturn(List.of());
    when(gameMapper.map(entity, List.of(), Map.of(), null)).thenReturn(game);

    assertThat(service.game("ARCHIVE")).isSameAs(game);
  }

  @Test
  void movesMapsStoredMoves() {
    UUID gameId = UUID.randomUUID();
    List<GameMoveEntity> moves = List.of();
    List<GameMove> mapped = List.of(GameMove.newBuilder().ply(0).move(1234L).build());
    when(gameService.findMoves(gameId)).thenReturn(moves);
    when(gameMapper.mapMoveEntities(moves)).thenReturn(mapped);

    assertThat(service.moves(gameId.toString())).isSameAs(mapped);
  }

  @Test
  void playersMapsAccountsInRequestOrder() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(second);
    account.setUsername("black-player");
    when(accountService.findAllByIds(List.of(first, second))).thenReturn(List.of(account));

    var players = service.players(List.of(first.toString(), second.toString()));

    assertThat(players).hasSize(2);
    assertThat(players.get(0).getUsername()).isEqualTo(first.toString());
    assertThat(players.get(1).getUsername()).isEqualTo("black-player");
  }

  @Test
  void gameReturnsNullWhenMissing() {
    when(gameService.findByCode("MISSING")).thenReturn(Optional.empty());

    assertThat(service.game("MISSING")).isNull();
  }

  @Test
  void myGamesUsesCallerAndDefaultSort() {
    UUID caller = UUID.randomUUID();
    UserContextHolder.setUserId(caller);
    GameEntity entity = new GameEntity();
    Game game = Game.newBuilder().id("game").build();
    Page<GameEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(1, 10), 21);
    when(gameService.findForAccount(eq(caller), any(Pageable.class))).thenReturn(page);
    when(gameService.recordForAccount(caller)).thenReturn(new GameService.GameTotals(1, 2, 3));
    when(accountService.findAllByIds(List.of())).thenReturn(List.of());
    when(gameMapper.map(entity, List.of(), Map.of(), caller)).thenReturn(game);

    GamePage result = service.myGames(
        new PaginationInput(1, 10, null, null, null, null));

    assertThat(result.getItems()).containsExactly(game);
    assertThat(result.getRecord().getPlayed()).isEqualTo(6);
    assertThat(result.getPageInfo().getPage()).isEqualTo(1);
    assertThat(result.getPageInfo().getTotalPages()).isEqualTo(3);
    assertThat(result.getPageInfo().getHasNextPage()).isTrue();
    assertThat(result.getPageInfo().getHasPreviousPage()).isTrue();

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(gameService).findForAccount(eq(caller), captor.capture());
    assertThat(captor.getValue().getSort().getOrderFor("finishedAt").getDirection())
        .isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void myGamesUsesRequestedSort() {
    UUID caller = UUID.randomUUID();
    UserContextHolder.setUserId(caller);
    Page<GameEntity> page = Page.empty();
    when(gameService.findForAccount(eq(caller), any(Pageable.class))).thenReturn(page);
    when(gameService.recordForAccount(caller)).thenReturn(new GameService.GameTotals(0, 0, 0));

    service.myGames(new PaginationInput(0, 5, "createdAt", SortDirection.ASC, null, null));

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(gameService).findForAccount(eq(caller), captor.capture());
    assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection())
        .isEqualTo(Sort.Direction.ASC);
  }

  @Test
  void myGamesRequiresCaller() {
    assertThatThrownBy(() -> service.myGames(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("authenticated account");
  }

  @Test
  void archiveGameMapsInputAndMoves() {
    GameArchiveInput input = GameArchiveInput.newBuilder().build();
    GameEntity entity = new GameEntity();
    List<GameMoveEntity> moves = List.of();
    GameEntity saved = new GameEntity();
    saved.setId(UUID.randomUUID());
    Game game = Game.newBuilder().id(saved.getId().toString()).build();
    when(gameMapper.mapInput(input)).thenReturn(entity);
    when(gameMapper.mapMoves(input)).thenReturn(moves);
    when(gameService.archive(entity, moves)).thenReturn(saved);
    when(gameMapper.map(saved, List.of())).thenReturn(game);

    assertThat(service.archiveGame(input)).isSameAs(game);
  }
}

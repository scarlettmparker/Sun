package com.sun.medusa.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.medusa.codegen.types.Game;
import com.sun.medusa.codegen.types.GameArchiveInput;
import com.sun.medusa.codegen.types.GameMove;
import com.sun.medusa.codegen.types.GamePage;
import com.sun.medusa.codegen.types.GameResponse;
import com.sun.medusa.codegen.types.PaginationInput;
import com.sun.medusa.graphql.services.GameGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameDataFetcherTest {

  @Mock private GameGraphQLService gameGraphQLService;

  @InjectMocks private GameDataFetcher fetcher;

  @Test
  void gameDelegatesToService() {
    Game game = Game.newBuilder().id("game").build();
    when(gameGraphQLService.game("ARCHIVE")).thenReturn(game);

    assertThat(fetcher.game("ARCHIVE")).isSameAs(game);
    verify(gameGraphQLService).game("ARCHIVE");
  }

  @Test
  void myGamesDelegatesToService() {
    PaginationInput pagination = new PaginationInput();
    GamePage page = GamePage.newBuilder().build();
    when(gameGraphQLService.myGames(pagination)).thenReturn(page);

    assertThat(fetcher.myGames(pagination)).isSameAs(page);
    verify(gameGraphQLService).myGames(pagination);
  }

  @Test
  void movesDelegatesToService() {
    Game game = Game.newBuilder().id("game-id").build();
    DgsDataFetchingEnvironment env = mock(DgsDataFetchingEnvironment.class);
    List<GameMove> moves = List.of(GameMove.newBuilder().ply(0).move(1234L).build());
    when(env.getSource()).thenReturn(game);
    when(gameGraphQLService.moves("game-id")).thenReturn(moves);

    assertThat(fetcher.moves(env)).isSameAs(moves);
    verify(gameGraphQLService).moves("game-id");
  }

  @Test
  void movesReturnsEmptyWithoutSource() {
    DgsDataFetchingEnvironment env = mock(DgsDataFetchingEnvironment.class);
    when(env.getSource()).thenReturn(null);

    assertThat(fetcher.moves(env)).isEmpty();
  }

  @Test
  void archiveGameBuildsResponse() {
    GameArchiveInput input = GameArchiveInput.newBuilder().build();
    Game game = Game.newBuilder().id("game").build();
    when(gameGraphQLService.archiveGame(input)).thenReturn(game);

    GameResponse result = fetcher.archiveGame(input);

    assertThat(result.getMessage()).isEqualTo("Game archived");
    assertThat(result.getGame()).isSameAs(game);
  }
}

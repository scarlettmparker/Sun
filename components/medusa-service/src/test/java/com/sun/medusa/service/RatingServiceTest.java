package com.sun.medusa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.medusa.model.PlayerRatingEntity;
import com.sun.medusa.model.RatingHistoryEntity;
import com.sun.medusa.rating.RatingResult;
import com.sun.medusa.repository.PlayerRatingRepository;
import com.sun.medusa.repository.RatingHistoryRepository;
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

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

  @Mock private PlayerRatingRepository ratingRepository;
  @Mock private RatingHistoryRepository historyRepository;

  @InjectMocks private RatingService service;

  @Test
  void fillsMissingRatingsWithDefaults() {
    UUID accountId = UUID.randomUUID();
    when(ratingRepository.findAllByAccountIds(List.of(accountId))).thenReturn(List.of());

    List<PlayerRatingEntity> ratings = service.ratingsFor(List.of(accountId));

    assertThat(ratings).hasSize(1);
    assertThat(ratings.get(0).getRating()).isEqualTo(1500);
    assertThat(ratings.get(0).getDeviation()).isEqualTo(350);
  }

  @Test
  void appliesAWinToBothPlayers() {
    UUID gameId = UUID.randomUUID();
    UUID whiteId = UUID.randomUUID();
    UUID blackId = UUID.randomUUID();
    PlayerRatingEntity white = PlayerRatingEntity.defaults(whiteId);
    PlayerRatingEntity black = PlayerRatingEntity.defaults(blackId);
    when(historyRepository.findByGameId(gameId)).thenReturn(List.of());
    when(historyRepository.countPairGames(eq(whiteId), eq(blackId), any()))
        .thenReturn(0L);
    when(ratingRepository.findForUpdate(whiteId)).thenReturn(Optional.of(white));
    when(ratingRepository.findForUpdate(blackId)).thenReturn(Optional.of(black));

    RatingResult result = service.applyResult(
        gameId, whiteId, blackId, "white-checkmate", LocalDateTime.now());

    assertThat(result.applied()).isTrue();
    assertThat(result.changes()).hasSize(2);
    assertThat(result.changes().get(0).delta()).isPositive();
    assertThat(result.changes().get(1).delta()).isNegative();
    assertThat(white.getRatedGames()).isEqualTo(1);
    assertThat(black.getRatedGames()).isEqualTo(1);

    ArgumentCaptor<RatingHistoryEntity> history =
        ArgumentCaptor.forClass(RatingHistoryEntity.class);
    verify(historyRepository, org.mockito.Mockito.times(2)).save(history.capture());
    assertThat(history.getAllValues()).extracting(RatingHistoryEntity::getOpponentAccountId)
        .containsExactlyInAnyOrder(whiteId, blackId);
  }

  @Test
  void skipsAbortedGames() {
    RatingResult result = service.applyResult(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "aborted", LocalDateTime.now());

    assertThat(result.applied()).isFalse();
    assertThat(result.unratedReason()).isEqualTo(RatingService.REASON_ABORTED);
    verify(ratingRepository, never()).save(any());
  }

  @Test
  void enforcesThePairCap() {
    UUID gameId = UUID.randomUUID();
    UUID whiteId = UUID.randomUUID();
    UUID blackId = UUID.randomUUID();
    when(historyRepository.findByGameId(gameId)).thenReturn(List.of());
    when(historyRepository.countPairGames(eq(whiteId), eq(blackId), any()))
        .thenReturn(5L);

    RatingResult result = service.applyResult(
        gameId, whiteId, blackId, "draw", LocalDateTime.now());

    assertThat(result.applied()).isFalse();
    assertThat(result.unratedReason()).isEqualTo(RatingService.REASON_PAIR_CAP);
    verify(ratingRepository, never()).findForUpdate(any());
  }

  @Test
  void duplicateGamesReturnStoredChanges() {
    UUID gameId = UUID.randomUUID();
    RatingHistoryEntity history = new RatingHistoryEntity();
    history.setAccountId(UUID.randomUUID());
    history.setOpponentAccountId(UUID.randomUUID());
    history.setGameId(gameId);
    history.setResult("win");
    history.setRatingBefore(1500);
    history.setRatingAfter(1512);
    history.setDeviationBefore(200);
    history.setDeviationAfter(190);
    history.setDelta(12);
    when(historyRepository.findByGameId(gameId)).thenReturn(List.of(history));

    RatingResult result = service.applyResult(
        gameId, UUID.randomUUID(), UUID.randomUUID(), "white-checkmate", LocalDateTime.now());

    assertThat(result.applied()).isFalse();
    assertThat(result.unratedReason()).isEqualTo(RatingService.REASON_DUPLICATE);
    assertThat(result.changes()).hasSize(1);
    assertThat(result.changes().get(0).delta()).isEqualTo(12);
    verify(ratingRepository, never()).findForUpdate(any());
  }

  @Test
  void leaderboardUsesTheMinimumGameThreshold() {
    when(ratingRepository.findTopRated(eq(RatingService.LEADERBOARD_MIN_GAMES), any()))
        .thenReturn(List.of());

    service.leaderboard(20);

    verify(ratingRepository).findTopRated(eq(RatingService.LEADERBOARD_MIN_GAMES), any());
    verify(ratingRepository, never()).findAllByAccountIds(any());
  }
}

package com.sun.medusa.repository;

import com.sun.medusa.model.RatingHistoryEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingHistoryRepository extends JpaRepository<RatingHistoryEntity, UUID> {

  List<RatingHistoryEntity> findByGameId(UUID gameId);

  boolean existsByGameId(UUID gameId);

  @Query("""
      SELECT COUNT(h) FROM RatingHistoryEntity h
      WHERE h.accountId = :accountId
        AND h.opponentAccountId = :opponentId
        AND h.createdAt >= :since
      """)
  long countPairGames(
      @Param("accountId") UUID accountId,
      @Param("opponentId") UUID opponentId,
      @Param("since") LocalDateTime since);

  @Query("""
      SELECT h FROM RatingHistoryEntity h
      WHERE h.accountId = :accountId
      ORDER BY h.createdAt DESC
      """)
  Page<RatingHistoryEntity> findForAccount(
      @Param("accountId") UUID accountId,
      Pageable pageable);
}

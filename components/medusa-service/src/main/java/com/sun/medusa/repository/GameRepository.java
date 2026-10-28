package com.sun.medusa.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.medusa.model.GameEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends BaseRepository<GameEntity> {
  Optional<GameEntity> findByCode(String code);

  @Query("""
      SELECT g FROM GameEntity g
      WHERE g.creatorAccountId = :accountId
         OR g.whiteAccountId = :accountId
         OR g.blackAccountId = :accountId
      """)
  Page<GameEntity> findForAccount(@Param("accountId") UUID accountId, Pageable pageable);

  @Query("""
      SELECT
        COALESCE(SUM(CASE WHEN g.winnerAccountId = :accountId THEN 1 ELSE 0 END), 0) AS wins,
        COALESCE(SUM(CASE WHEN g.winnerAccountId IS NOT NULL
          AND g.winnerAccountId <> :accountId THEN 1 ELSE 0 END), 0) AS losses,
        COALESCE(SUM(CASE WHEN g.winnerAccountId IS NULL THEN 1 ELSE 0 END), 0) AS draws
      FROM GameEntity g
      WHERE g.whiteAccountId = :accountId OR g.blackAccountId = :accountId
      """)
  GameRecordProjection recordForAccount(@Param("accountId") UUID accountId);
}

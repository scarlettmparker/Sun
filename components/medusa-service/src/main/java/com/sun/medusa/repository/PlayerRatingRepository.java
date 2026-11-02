package com.sun.medusa.repository;

import com.sun.medusa.model.PlayerRatingEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRatingRepository extends JpaRepository<PlayerRatingEntity, UUID> {

  @Query("""
      SELECT r FROM PlayerRatingEntity r
      WHERE r.accountId IN :accountIds
      """)
  List<PlayerRatingEntity> findAllByAccountIds(@Param("accountIds") Collection<UUID> accountIds);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT r FROM PlayerRatingEntity r
      WHERE r.accountId = :accountId
      """)
  Optional<PlayerRatingEntity> findForUpdate(@Param("accountId") UUID accountId);

  @Query("""
      SELECT r FROM PlayerRatingEntity r
      WHERE r.ratedGames >= :minGames
      ORDER BY r.rating DESC, r.ratedGames DESC
      """)
  List<PlayerRatingEntity> findTopRated(@Param("minGames") int minGames, Pageable pageable);
}

package com.sun.medusa.repository;

import com.sun.medusa.model.GameMoveEntity;
import com.sun.medusa.model.GameMoveId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameMoveRepository extends JpaRepository<GameMoveEntity, GameMoveId> {
  @Query("""
      SELECT m FROM GameMoveEntity m
      WHERE m.id.gameId = :gameId
      ORDER BY m.id.ply ASC
      """)
  List<GameMoveEntity> findByGameId(@Param("gameId") UUID gameId);
}

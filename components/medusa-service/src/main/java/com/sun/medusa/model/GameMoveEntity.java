package com.sun.medusa.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * One move in an archived game.
 */
@Entity
@Table(name = "medusa_game_moves")
public class GameMoveEntity {

  @EmbeddedId
  private GameMoveId id;

  @Column(name = "move", nullable = false)
  private long move;

  @Column(name = "played_at", nullable = false)
  private LocalDateTime playedAt;

  public GameMoveEntity() {}

  public GameMoveEntity(GameMoveId id, long move, LocalDateTime playedAt) {
    this.id = id;
    this.move = move;
    this.playedAt = playedAt;
  }

  public GameMoveId getId() {
    return id;
  }

  public void setId(GameMoveId id) {
    this.id = id;
  }

  public long getMove() {
    return move;
  }

  public void setMove(long move) {
    this.move = move;
  }

  public LocalDateTime getPlayedAt() {
    return playedAt;
  }

  public void setPlayedAt(LocalDateTime playedAt) {
    this.playedAt = playedAt;
  }
}

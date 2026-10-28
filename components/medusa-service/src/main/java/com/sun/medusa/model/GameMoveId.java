package com.sun.medusa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class GameMoveId implements Serializable {

  @Column(name = "game_id")
  private UUID gameId;

  @Column(name = "ply")
  private int ply;

  public GameMoveId() {}

  public GameMoveId(UUID gameId, int ply) {
    this.gameId = gameId;
    this.ply = ply;
  }

  public UUID getGameId() {
    return gameId;
  }

  public void setGameId(UUID gameId) {
    this.gameId = gameId;
  }

  public int getPly() {
    return ply;
  }

  public void setPly(int ply) {
    this.ply = ply;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GameMoveId)) return false;
    GameMoveId that = (GameMoveId) o;
    return ply == that.ply && Objects.equals(gameId, that.gameId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gameId, ply);
  }
}

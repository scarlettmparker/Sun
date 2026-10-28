package com.sun.medusa.model;

import com.sun.base.model.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Type;

/**
 * Archived record of one finished game.
 */
@Entity
@Table(name = "medusa_games")
public class GameEntity extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true, length = 7)
  private String code;

  @Column(name = "board_layout_id")
  private UUID boardLayoutId;

  @Column(name = "board_layout_name", nullable = false)
  private String boardLayoutName;

  @Column(name = "status", nullable = false)
  private String status;

  @Type(JsonBinaryType.class)
  @Column(name = "initial_state", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> initialState;

  @Type(JsonBinaryType.class)
  @Column(name = "final_state", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> finalState;

  @Column(name = "creator_account_id", nullable = false)
  private UUID creatorAccountId;

  @Column(name = "white_account_id")
  private UUID whiteAccountId;

  @Column(name = "black_account_id")
  private UUID blackAccountId;

  @Column(name = "result")
  private String result;

  @Column(name = "winner_account_id")
  private UUID winnerAccountId;

  @Column(name = "move_count", nullable = false)
  private int moveCount;

  @Column(name = "game_created_at", nullable = false)
  private LocalDateTime gameCreatedAt;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "finished_at", nullable = false)
  private LocalDateTime finishedAt;

  @Column(name = "archived_at", nullable = false)
  private LocalDateTime archivedAt;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public UUID getBoardLayoutId() {
    return boardLayoutId;
  }

  public void setBoardLayoutId(UUID boardLayoutId) {
    this.boardLayoutId = boardLayoutId;
  }

  public String getBoardLayoutName() {
    return boardLayoutName;
  }

  public void setBoardLayoutName(String boardLayoutName) {
    this.boardLayoutName = boardLayoutName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Map<String, Object> getInitialState() {
    return initialState;
  }

  public void setInitialState(Map<String, Object> initialState) {
    this.initialState = initialState;
  }

  public Map<String, Object> getFinalState() {
    return finalState;
  }

  public void setFinalState(Map<String, Object> finalState) {
    this.finalState = finalState;
  }

  public UUID getCreatorAccountId() {
    return creatorAccountId;
  }

  public void setCreatorAccountId(UUID creatorAccountId) {
    this.creatorAccountId = creatorAccountId;
  }

  public UUID getWhiteAccountId() {
    return whiteAccountId;
  }

  public void setWhiteAccountId(UUID whiteAccountId) {
    this.whiteAccountId = whiteAccountId;
  }

  public UUID getBlackAccountId() {
    return blackAccountId;
  }

  public void setBlackAccountId(UUID blackAccountId) {
    this.blackAccountId = blackAccountId;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public UUID getWinnerAccountId() {
    return winnerAccountId;
  }

  public void setWinnerAccountId(UUID winnerAccountId) {
    this.winnerAccountId = winnerAccountId;
  }

  public int getMoveCount() {
    return moveCount;
  }

  public void setMoveCount(int moveCount) {
    this.moveCount = moveCount;
  }

  public LocalDateTime getGameCreatedAt() {
    return gameCreatedAt;
  }

  public void setGameCreatedAt(LocalDateTime gameCreatedAt) {
    this.gameCreatedAt = gameCreatedAt;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public LocalDateTime getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(LocalDateTime finishedAt) {
    this.finishedAt = finishedAt;
  }

  public LocalDateTime getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(LocalDateTime archivedAt) {
    this.archivedAt = archivedAt;
  }
}

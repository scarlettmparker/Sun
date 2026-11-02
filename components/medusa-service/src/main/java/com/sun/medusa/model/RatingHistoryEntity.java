package com.sun.medusa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One rating change recorded for one account in one game.
 */
@Entity
@Table(
    name = "medusa_rating_history",
    uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "account_id"}))
public class RatingHistoryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "opponent_account_id", nullable = false)
  private UUID opponentAccountId;

  @Column(name = "game_id", nullable = false)
  private UUID gameId;

  @Column(name = "result", nullable = false)
  private String result;

  @Column(name = "rating_before", nullable = false)
  private double ratingBefore;

  @Column(name = "rating_after", nullable = false)
  private double ratingAfter;

  @Column(name = "deviation_before", nullable = false)
  private double deviationBefore;

  @Column(name = "deviation_after", nullable = false)
  private double deviationAfter;

  @Column(name = "delta", nullable = false)
  private double delta;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public UUID getOpponentAccountId() {
    return opponentAccountId;
  }

  public void setOpponentAccountId(UUID opponentAccountId) {
    this.opponentAccountId = opponentAccountId;
  }

  public UUID getGameId() {
    return gameId;
  }

  public void setGameId(UUID gameId) {
    this.gameId = gameId;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public double getRatingBefore() {
    return ratingBefore;
  }

  public void setRatingBefore(double ratingBefore) {
    this.ratingBefore = ratingBefore;
  }

  public double getRatingAfter() {
    return ratingAfter;
  }

  public void setRatingAfter(double ratingAfter) {
    this.ratingAfter = ratingAfter;
  }

  public double getDeviationBefore() {
    return deviationBefore;
  }

  public void setDeviationBefore(double deviationBefore) {
    this.deviationBefore = deviationBefore;
  }

  public double getDeviationAfter() {
    return deviationAfter;
  }

  public void setDeviationAfter(double deviationAfter) {
    this.deviationAfter = deviationAfter;
  }

  public double getDelta() {
    return delta;
  }

  public void setDelta(double delta) {
    this.delta = delta;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}

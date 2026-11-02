package com.sun.medusa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stored Glicko-2 rating for one Free Chess account.
 */
@Entity
@Table(name = "medusa_player_ratings")
public class PlayerRatingEntity {

  @Id
  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "rating", nullable = false)
  private double rating;

  @Column(name = "deviation", nullable = false)
  private double deviation;

  @Column(name = "volatility", nullable = false)
  private double volatility;

  @Column(name = "rated_games", nullable = false)
  private int ratedGames;

  @Column(name = "peak_rating", nullable = false)
  private double peakRating;

  @Column(name = "last_rated_at")
  private LocalDateTime lastRatedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Creates a rating with the default Glicko-2 values.
   *
   * @param accountId account id
   * @return default rating
   */
  public static PlayerRatingEntity defaults(UUID accountId) {
    LocalDateTime now = LocalDateTime.now();
    PlayerRatingEntity entity = new PlayerRatingEntity();
    entity.setAccountId(accountId);
    entity.setRating(1500);
    entity.setDeviation(350);
    entity.setVolatility(0.06);
    entity.setRatedGames(0);
    entity.setPeakRating(1500);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    return entity;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public double getRating() {
    return rating;
  }

  public void setRating(double rating) {
    this.rating = rating;
  }

  public double getDeviation() {
    return deviation;
  }

  public void setDeviation(double deviation) {
    this.deviation = deviation;
  }

  public double getVolatility() {
    return volatility;
  }

  public void setVolatility(double volatility) {
    this.volatility = volatility;
  }

  public int getRatedGames() {
    return ratedGames;
  }

  public void setRatedGames(int ratedGames) {
    this.ratedGames = ratedGames;
  }

  public double getPeakRating() {
    return peakRating;
  }

  public void setPeakRating(double peakRating) {
    this.peakRating = peakRating;
  }

  public LocalDateTime getLastRatedAt() {
    return lastRatedAt;
  }

  public void setLastRatedAt(LocalDateTime lastRatedAt) {
    this.lastRatedAt = lastRatedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}

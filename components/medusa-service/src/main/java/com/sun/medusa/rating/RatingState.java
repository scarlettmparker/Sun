package com.sun.medusa.rating;

/**
 * Glicko-2 state for one player.
 *
 * @param rating Elo-scale rating
 * @param deviation rating deviation
 * @param volatility rating volatility
 */
public record RatingState(double rating, double deviation, double volatility) {

  public static final double DEFAULT_RATING = 1500;
  public static final double DEFAULT_DEVIATION = 350;
  public static final double DEFAULT_VOLATILITY = 0.06;

  /**
   * Builds the default unrated player state.
   *
   * @return default state
   */
  public static RatingState defaults() {
    return new RatingState(DEFAULT_RATING, DEFAULT_DEVIATION, DEFAULT_VOLATILITY);
  }

  /**
   * Whether this rating is still provisional.
   *
   * @return true when the rating deviation is still high
   */
  public boolean provisional() {
    return deviation > 110;
  }
}

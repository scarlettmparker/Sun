package com.sun.medusa.rating;

import java.util.function.DoubleUnaryOperator;

/**
 * Glicko-2 calculator using fractional rating periods.
 */
public class Glicko2Calculator {

  private static final double SCALE = 173.7178;
  private static final double MIN_RATING = 400;
  private static final double MAX_RATING = 3500;
  private static final double MIN_DEVIATION = 45;
  private static final double MAX_DEVIATION = 350;
  private static final double MAX_VOLATILITY = 0.1;
  private static final double RATING_PERIODS_PER_DAY = 0.21436;
  private static final double EPSILON = 0.000001;

  private final double tau;

  /**
   * Creates a calculator with the standard volatility constraint.
   */
  public Glicko2Calculator() {
    this(0.5);
  }

  /**
   * Creates a calculator with a custom volatility constraint.
   *
   * @param tau volatility constraint
   */
  public Glicko2Calculator(double tau) {
    this.tau = tau;
  }

  /**
   * Updates one player's rating for a single game.
   *
   * @param player player before the game
   * @param opponent opponent before the game
   * @param score 1 for a win, 0.5 for a draw, 0 for a loss
   * @param elapsedDays days since the player's previous rated game
   * @return updated player state
   */
  public RatingState update(
      RatingState player,
      RatingState opponent,
      double score,
      double elapsedDays) {
    if (score < 0 || score > 1) {
      throw new IllegalArgumentException("Score must be between zero and one");
    }

    double mu = (player.rating() - RatingState.DEFAULT_RATING) / SCALE;
    double phi = player.deviation() / SCALE;
    double phiOpponent = opponent.deviation() / SCALE;
    double muOpponent = (opponent.rating() - RatingState.DEFAULT_RATING) / SCALE;

    double impact = 1.0 / Math.sqrt(1.0 + (3.0 * phiOpponent * phiOpponent) / (Math.PI * Math.PI));
    double expected = 1.0 / (1.0 + Math.exp(-impact * (mu - muOpponent)));
    expected = Math.max(EPSILON, Math.min(1.0 - EPSILON, expected));

    double variance = 1.0 / (impact * impact * expected * (1.0 - expected));
    double difference = variance * impact * (score - expected);
    double sigma = determineSigma(phi, variance, difference, player.volatility());
    double periods = Math.max(0, elapsedDays) * RATING_PERIODS_PER_DAY;
    double phiStar = Math.sqrt(phi * phi + periods * sigma * sigma);
    double phiPrime = 1.0 / Math.sqrt(1.0 / (phiStar * phiStar) + 1.0 / variance);
    double muPrime = mu + phiPrime * phiPrime * (difference / variance);

    return new RatingState(
        clamp(muPrime * SCALE + RatingState.DEFAULT_RATING, MIN_RATING, MAX_RATING),
        clamp(phiPrime * SCALE, MIN_DEVIATION, MAX_DEVIATION),
        Math.min(sigma, MAX_VOLATILITY));
  }

  /**
   * Solves for the new volatility using the Illinois algorithm.
   *
   * @param phi rating deviation on the Glicko-2 scale
   * @param variance estimated variance
   * @param difference estimated improvement
   * @param sigma current volatility
   * @return new volatility
   */
  private double determineSigma(double phi, double variance, double difference, double sigma) {
    double alpha = Math.log(sigma * sigma);
    double differenceSquared = difference * difference;
    double phiSquared = phi * phi;

    DoubleUnaryOperator objective = x -> {
      double expX = Math.exp(x);
      double tmp = phiSquared + variance + expX;
      double left = expX * (differenceSquared - tmp) / (2 * tmp * tmp);
      double right = (x - alpha) / (tau * tau);
      return left - right;
    };

    double a = alpha;
    double b;
    if (differenceSquared > phiSquared + variance) {
      b = Math.log(differenceSquared - phiSquared - variance);
    } else {
      double k = 1;
      while (objective.applyAsDouble(alpha - k * tau) < 0) {
        k += 1;
      }
      b = alpha - k * tau;
    }

    double fA = objective.applyAsDouble(a);
    double fB = objective.applyAsDouble(b);

    while (Math.abs(b - a) > EPSILON) {
      double c = a + (a - b) * fA / (fB - fA);
      double fC = objective.applyAsDouble(c);
      if (fC * fB < 0) {
        a = b;
        fA = fB;
      } else {
        fA = fA / 2;
      }
      b = c;
      fB = fC;
    }

    return Math.exp(a / 2);
  }

  /**
   * Clamps a value to a range.
   *
   * @param value value to clamp
   * @param min minimum
   * @param max maximum
   * @return clamped value
   */
  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}

package com.sun.medusa.rating;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Glicko2CalculatorTest {

  private final Glicko2Calculator calculator = new Glicko2Calculator();

  @Test
  void matchesThePublishedExample() {
    RatingState updated = calculator.update(
        new RatingState(1500, 200, 0.06),
        new RatingState(1400, 30, 0.06),
        1,
        0);

    assertThat(updated.rating()).isCloseTo(1563.43, within(0.15));
    assertThat(updated.deviation()).isCloseTo(175.22, within(0.15));
    assertThat(updated.volatility()).isCloseTo(0.05999, within(0.0001));
  }

  @Test
  void equalDrawDoesNotMoveRating() {
    RatingState updated = calculator.update(
        new RatingState(1500, 200, 0.06),
        new RatingState(1500, 200, 0.06),
        0.5,
        0);

    assertThat(updated.rating()).isCloseTo(1500, within(0.001));
    assertThat(updated.deviation()).isLessThan(200);
  }

  @Test
  void inactivityIncreasesRatingDeviation() {
    RatingState opponent = new RatingState(1500, 60, 0.06);
    RatingState fresh = calculator.update(new RatingState(1500, 60, 0.06), opponent, 0.5, 0);
    RatingState inactive = calculator.update(new RatingState(1500, 60, 0.06), opponent, 0.5, 30);

    assertThat(inactive.deviation()).isGreaterThan(fresh.deviation());
  }

  private static org.assertj.core.data.Offset<Double> within(double value) {
    return org.assertj.core.data.Offset.offset(value);
  }
}

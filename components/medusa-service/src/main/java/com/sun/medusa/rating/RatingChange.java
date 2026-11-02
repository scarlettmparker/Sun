package com.sun.medusa.rating;

import java.util.UUID;

/**
 * Rating change for one account.
 *
 * @param accountId account id
 * @param ratingBefore rating before the game
 * @param ratingAfter rating after the game
 * @param deviationBefore deviation before the game
 * @param deviationAfter deviation after the game
 * @param delta rating change
 */
public record RatingChange(
    UUID accountId,
    double ratingBefore,
    double ratingAfter,
    double deviationBefore,
    double deviationAfter,
    double delta) {
}

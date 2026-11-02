package com.sun.medusa.rating;

import java.util.List;

/**
 * Outcome of applying ratings for one game.
 *
 * @param applied whether new rating history was written
 * @param unratedReason reason the game was not rated, or null
 * @param changes rating changes for both players
 */
public record RatingResult(boolean applied, String unratedReason, List<RatingChange> changes) {
}

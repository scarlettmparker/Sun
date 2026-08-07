package com.sun.graphql.config;

/**
 * Thread-safe in-memory token bucket for a single rate-limit key.
 */
final class TokenBucket {

  private final int capacity;
  private final double refillPerSecond;
  private volatile double tokens;
  private volatile long lastRefillNanos;

  TokenBucket(int capacity, double refillPerSecond) {
    this.capacity = capacity;
    this.refillPerSecond = refillPerSecond;
    this.tokens = capacity;
    this.lastRefillNanos = System.nanoTime();
  }

  /**
   * Consumes one token if available, refilling first.
   *
   * @return true when a token was available
   */
  synchronized boolean tryAcquire() {
    refill();
    if (tokens >= 1.0) {
      tokens -= 1.0;
      return true;
    }
    return false;
  }

  /**
   * Seconds until the next token is available.
   *
   * @return the wait time, or zero when a token is ready
   */
  synchronized int retryAfterSeconds() {
    refill();
    if (tokens >= 1.0) {
      return 0;
    }
    double deficit = 1.0 - tokens;
    return (int) Math.ceil(deficit / refillPerSecond);
  }

  /**
   * Adds tokens for the elapsed time since the last refill.
   */
  private void refill() {
    long now = System.nanoTime();
    double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
    double gained = refillPerSecond * elapsedSeconds;
    tokens = Math.min(capacity, tokens + gained);
    lastRefillNanos = now;
  }
}

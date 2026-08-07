package com.sun.graphql.config;

/**
 * A configured token-bucket limit.
 */
public record RateLimitConfig(
    /**
     * Token capacity (maximum instant burst size).
     */
    int capacity,
    /**
     * Tokens refilled per second.
     */
    double refillPerSecond) {}

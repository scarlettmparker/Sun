package com.sun.graphql.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Default rate-limit configuration for the GraphQL gateway.
 */
@ConfigurationProperties(prefix = "ratelimit")
public record RateLimitProperties(
    /**
     * Whether the rate limiter is active.
     */
    boolean enabled,
    /**
     * Default token capacity (maximum instant burst size).
     */
    int defaultCapacity,
    /**
     * Default tokens refilled per second.
     */
    double defaultRefillPerSecond) {}

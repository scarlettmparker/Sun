package com.sun.graphql.config;

/**
 * The bucket a request is charged against and its limit.
 */
public record RateLimitBinding(
    /**
     * Shared bucket key for matching operations.
     */
    String bucket,
    /**
     * The limit applied to that bucket.
     */
    RateLimitConfig config) {}

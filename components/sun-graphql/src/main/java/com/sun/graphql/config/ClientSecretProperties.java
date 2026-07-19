package com.sun.graphql.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-app secrets the frontend servers present to reach the gateway.
 */
@ConfigurationProperties(prefix = "client")
public record ClientSecretProperties(
    /**
     * Expected secret keyed by app id (matched against the X-Client-Id header).
     */
    Map<String, String> secrets) {}

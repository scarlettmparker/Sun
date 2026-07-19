package com.sun.graphql.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the per-app client secret properties used by {@link ClientSecretFilter}.
 */
@Configuration
@EnableConfigurationProperties(ClientSecretProperties.class)
public class ClientSecurityConfig {}

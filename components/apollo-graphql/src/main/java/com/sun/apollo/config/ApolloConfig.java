package com.sun.apollo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module entry point. Disables the entire module when
 * {@code apollo.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(prefix = "apollo", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sun.apollo")
public class ApolloConfig {}

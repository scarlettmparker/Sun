package com.sun.fates.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module entry point. Disables the entire module when
 * {@code fates.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(prefix = "fates", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sun.fates")
public class FatesConfig {}

package com.sun.briareus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module entry point. Disables the entire module when
 * {@code briareus.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(prefix = "briareus", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sun.briareus")
public class BriareusConfig {}

package com.sun.dionysus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module entry point. Disables the entire module when
 * {@code dionysus.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(prefix = "dionysus", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sun.dionysus")
public class DionysusConfig {}

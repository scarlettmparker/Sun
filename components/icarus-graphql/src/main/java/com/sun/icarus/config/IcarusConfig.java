package com.sun.icarus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module entry point. Disables the entire module when
 * {@code icarus.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(prefix = "icarus", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sun.icarus")
public class IcarusConfig {}

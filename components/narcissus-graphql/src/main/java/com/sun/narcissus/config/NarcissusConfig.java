package com.sun.narcissus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module entry point. Disables the entire module when
 * {@code narcissus.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(prefix = "narcissus", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sun.narcissus")
public class NarcissusConfig {}

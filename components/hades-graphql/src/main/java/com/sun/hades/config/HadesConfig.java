package com.sun.hades.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module entry point. Disables the entire module when
 * {@code hades.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(prefix = "hades", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sun.hades")
public class HadesConfig {}

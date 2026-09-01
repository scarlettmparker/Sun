package com.sun.jocasta.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module entry point for Jocasta.
 */
@Configuration
@ConditionalOnProperty(prefix = "jocasta", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sun.jocasta")
public class JocastaConfig {}

package com.sun.cerberus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module entry point. Disables the entire module when
 * {@code cerberus.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(prefix = "cerberus", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sun.cerberus")
public class CerberusConfig {}

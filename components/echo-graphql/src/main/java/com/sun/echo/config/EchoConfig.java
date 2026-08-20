package com.sun.echo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module entry point. Disables the entire module when
 * {@code echo.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(prefix = "echo", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.sun.echo")
public class EchoConfig {}

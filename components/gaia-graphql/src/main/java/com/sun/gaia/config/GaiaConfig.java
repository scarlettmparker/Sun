package com.sun.gaia.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Core auth module - always enabled, no conditional.
 */
@Configuration
@ComponentScan(basePackages = "com.sun.gaia")
public class GaiaConfig {}

package com.sun.jocasta.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduling for Jocasta's periodic jobs.
 */
@Configuration
@EnableScheduling
public class JocastaSchedulingConfig {
}

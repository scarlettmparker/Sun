package com.sun.graphql.audit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configures the audit subsystem - the async executor and properties.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(AuditProperties.class)
public class AuditConfig {

  /**
   * Bounded executor that keeps audit writes off the request thread.
   *
   * @return the audit thread-pool task executor
   */
  @Bean(name = "auditExecutor")
  public ThreadPoolTaskExecutor auditExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("audit-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(10);
    executor.initialize();
    return executor;
  }
}

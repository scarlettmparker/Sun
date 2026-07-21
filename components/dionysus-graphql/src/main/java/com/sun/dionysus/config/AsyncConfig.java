package com.sun.dionysus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async and scheduling support for the torrent client: a bounded executor for
 * downloads and uploads, and the scheduler for watchdogs.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

  /**
   * Executor used to run torrent downloads and completion uploads off the request thread.
   */
  @Bean(name = "torrentTaskExecutor")
  public Executor torrentTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(3);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(64);
    executor.setThreadNamePrefix("torrent-async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
  }
}

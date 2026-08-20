package com.sun.graphql.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables in-process read caching for hot GraphQL list queries.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /**
   * Caffeine-backed cache manager.
   */
  @Bean
  public CacheManager cacheManager(@Value("${spring.cache.caffeine.spec:}") String spec) {
    CaffeineCacheManager manager = new CaffeineCacheManager();
    if (!spec.isBlank()) {
      manager.setCacheSpecification(spec);
    }
    return manager;
  }
}

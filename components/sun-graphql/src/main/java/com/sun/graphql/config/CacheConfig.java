package com.sun.graphql.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
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
   * Caffeine-backed cache manager with per-cache TTLs.
   */
  @Bean
  public CacheManager cacheManager() {
    return new CaffeineCacheManager() {
      @Override
      protected Cache<Object, Object> createNativeCaffeineCache(String name) {
        if ("defineWord".equals(name)) {
          return Caffeine.newBuilder()
              .expireAfterWrite(Duration.ofHours(24))
              .maximumSize(2000)
              .build();
        }
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(1000)
            .build();
      }
    };
  }
}

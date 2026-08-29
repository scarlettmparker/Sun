package com.sun.graphql.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sun.base.cache.CaffeineSpec;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Enables in-process read caching for hot GraphQL list queries.
 */
@Configuration
@EnableCaching
public class CacheConfig implements ApplicationContextAware {

  private ApplicationContext ctx;
  private volatile Map<String, CaffeineSpec> cacheSpecs;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.ctx = applicationContext;
  }

  /**
   * Caffeine-backed cache manager with per-cache TTLs resolved from {@code @CaffeineSpec} on the
   * {@code @Cacheable} call site.
   */
  @Bean
  public CacheManager cacheManager() {
    return new CaffeineCacheManager() {
      @Override
      protected Cache<Object, Object> createNativeCaffeineCache(String name) {
        CaffeineSpec spec = resolveSpec(name);
        Duration ttl = spec == null ? Duration.ofSeconds(30) : parseTtl(spec.expireAfterWrite());
        int max = spec == null ? 1000 : spec.maximumSize();
        return Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(max).build();
      }
    };
  }

  /**
   * Resolves the spec for a cache.
   *
   * @param name the cache name
   * @return the spec or null
   */
  private CaffeineSpec resolveSpec(String name) {
    Map<String, CaffeineSpec> map = cacheSpecs;
    if (map == null) {
      synchronized (this) {
        map = cacheSpecs;
        if (map == null) {
          cacheSpecs = map = buildSpecMap();
        }
      }
    }
    return map.get(name);
  }

  /**
   * Builds the cache spec map.
   *
   * @return the name to spec map
   */
  private Map<String, CaffeineSpec> buildSpecMap() {
    Map<String, CaffeineSpec> map = new HashMap<>();
    if (ctx == null) {
      return map;
    }
    for (String beanName : ctx.getBeanDefinitionNames()) {
      if (beanName.contains("cacheManager") || beanName.contains("CacheConfig")) {
        continue;
      }
      Object bean;
      try {
        bean = ctx.getBean(beanName);
      } catch (Exception ignored) {
        continue;
      }
      Class<?> target = AopUtils.getTargetClass(bean);
      for (Method method : target.getMethods()) {
        Cacheable cacheable = AnnotatedElementUtils.findMergedAnnotation(method, Cacheable.class);
        if (cacheable == null) {
          continue;
        }
        CaffeineSpec spec = AnnotatedElementUtils.findMergedAnnotation(method, CaffeineSpec.class);
        if (spec == null) {
          spec = AnnotatedElementUtils.findMergedAnnotation(target, CaffeineSpec.class);
        }
        if (spec == null) {
          continue;
        }
        for (String cacheName : resolveCacheNames(cacheable)) {
          map.putIfAbsent(cacheName, spec);
        }
      }
    }
    return map;
  }

  /**
   * Resolves cache names from an annotation.
   *
   * @param cacheable the cacheable annotation
   * @return the cache names
   */
  private List<String> resolveCacheNames(Cacheable cacheable) {
    List<String> names = new ArrayList<>();
    for (String n : cacheable.value()) {
      if (!n.isBlank()) {
        names.add(n.trim());
      }
    }
    for (String n : cacheable.cacheNames()) {
      if (!n.isBlank()) {
        names.add(n.trim());
      }
    }
    return names;
  }

  /**
   * Parses a TTL string.
   *
   * @param raw the raw value
   * @return the duration
   */
  private static Duration parseTtl(String raw) {
    if (raw == null || raw.isBlank()) {
      return Duration.ofSeconds(30);
    }
    String v = raw.trim();
    try {
      if (v.matches("\\d+d")) {
        return Duration.ofDays(Long.parseLong(v.substring(0, v.length() - 1)));
      }
      if (v.matches("\\d+h")) {
        return Duration.ofHours(Long.parseLong(v.substring(0, v.length() - 1)));
      }
      if (v.matches("\\d+m")) {
        return Duration.ofMinutes(Long.parseLong(v.substring(0, v.length() - 1)));
      }
      if (v.matches("\\d+s")) {
        return Duration.ofSeconds(Long.parseLong(v.substring(0, v.length() - 1)));
      }
      return Duration.parse(v);
    } catch (Exception e) {
      return Duration.ofSeconds(30);
    }
  }
}

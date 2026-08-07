package com.sun.graphql.config;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.ratelimit.RateLimit;
import com.sun.gaia.service.PermissionService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Maps GraphQL operation names to their declared rate limits, discovered from
 * RateLimit annotations on data-fetcher methods. Matching is glob based,
 * mirroring the permission service.
 */
@Component
public class RateLimitRegistry implements SmartInitializingSingleton {

  private static final Logger logger = LoggerFactory.getLogger(RateLimitRegistry.class);

  private final ApplicationContext context;
  private final List<GlobEntry> entries = new ArrayList<>();

  /**
   * Builds the registry; the scan runs once every singleton is created.
   *
   * @param context the application context to discover data fetchers in
   */
  public RateLimitRegistry(ApplicationContext context) {
    this.context = context;
  }

  /**
   * Registers a glob for a binding.
   *
   * @param pattern the operation-name glob
   * @param binding the bucket and limit
   */
  void register(String pattern, RateLimitBinding binding) {
    entries.add(new GlobEntry(pattern, binding));
  }

  /**
   * Finds the first rate limit whose glob matches an operation name.
   *
   * @param operation the GraphQL operation name
   * @return the matching binding, or empty to use the default
   */
  public Optional<RateLimitBinding> forOperation(String operation) {
    for (GlobEntry entry : entries) {
      if (PermissionService.match(operation, entry.pattern)) {
        return Optional.of(entry.binding);
      }
    }
    return Optional.empty();
  }

  @Override
  public void afterSingletonsInstantiated() {
    Map<String, Object> fetchers = context.getBeansWithAnnotation(DgsComponent.class);
    for (Object fetcher : fetchers.values()) {
      for (Method method : fetcher.getClass().getMethods()) {
        DgsData data = method.getAnnotation(DgsData.class);
        RateLimit limit = method.getAnnotation(RateLimit.class);
        if (data == null || limit == null) {
          continue;
        }
        String field = data.field();
        String pattern = limit.name().isBlank() ? field : limit.name();
        RateLimitConfig config = new RateLimitConfig(limit.capacity(), limit.refillPerSecond());
        register(pattern, new RateLimitBinding(pattern, config));
        logger.info("Rate limited operation {} (glob {}) to {} tokens, {} refill/s",
            field, pattern, config.capacity(), config.refillPerSecond());
      }
    }
  }

  /**
   * A rate-limit glob and its binding.
   */
  private record GlobEntry(String pattern, RateLimitBinding binding) {}
}

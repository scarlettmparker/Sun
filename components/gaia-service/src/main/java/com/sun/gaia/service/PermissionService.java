package com.sun.gaia.service;

import com.sun.gaia.repository.AccountRepository;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Permission evaluator exposed to SpEL as {@code @permissions}.
 */
@Component("permissions")
public class PermissionService {

  private final AccountRepository accountRepository;

  private static final ConcurrentHashMap<String, Pattern> PATTERN_CACHE =
      new ConcurrentHashMap<>();

  private static final ThreadLocal<UUID> cachedAccount = new ThreadLocal<>();
  private static final ThreadLocal<Set<String>> cachedPatterns = new ThreadLocal<>();

  public PermissionService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  /**
   * Whether a caller is signed in.
   */
  public boolean isAuthenticated() {
    return UserContextHolder.getUserId() != null;
  }

  /**
   * Whether the caller's granted permission patterns cover the required one.
   */
  public boolean has(String required) {
    UUID accountId = UserContextHolder.getUserId();
    if (accountId == null) {
      return false;
    }
    Set<String> patterns = effectivePatterns(accountId);
    for (String pattern : patterns) {
      if (match(required, pattern)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Throws unless the caller holds the required permission.
   */
  public void require(String required) {
    if (!has(required)) {
      throw new AccessDeniedException("Missing permission: " + required);
    }
  }

  /**
   * Loads the caller's patterns, cached per thread while the account stays the
   * same so one request does a single lookup.
   */
  private Set<String> effectivePatterns(UUID accountId) {
    if (accountId.equals(cachedAccount.get())) {
      return cachedPatterns.get();
    }
    Set<String> patterns = Set.copyOf(accountRepository.findEffectivePermissions(accountId));
    cachedAccount.set(accountId);
    cachedPatterns.set(patterns);
    return patterns;
  }

  /**
   * Whether a pattern covers a permission; {@code *} is a glob, so
   * {@code graphql.*} matches {@code graphql.icarus.thread}.
   */
  public static boolean match(String requested, String pattern) {
    if (pattern.equals("*")) {
      return true;
    }
    Pattern compiled =
        PATTERN_CACHE.computeIfAbsent(pattern, PermissionService::compilePattern);
    return compiled.matcher(requested).matches();
  }

  private static Pattern compilePattern(String pattern) {
    String regex = pattern.replace(".", "\\.").replace("*", ".*");
    return Pattern.compile("^" + regex + "$");
  }
}

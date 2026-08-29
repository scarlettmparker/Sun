package com.sun.base.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CaffeineSpec {

  /**
   * Time-to-live, e.g. {@code 24h}, {@code 30s}, or ISO-8601 {@code PT24H}.
   */
  String expireAfterWrite() default "30s";

  /**
   * Maximum cache entries.
   */
  int maximumSize() default 1000;
}

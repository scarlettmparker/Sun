package com.sun.base.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a token-bucket rate limit for a data-fetcher method, overriding the
 * gateway default.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

  /**
   * Operation-name glob this limit applies to; empty means the annotated
   * method's field name exactly.
   */
  String name() default "";

  /**
   * Token capacity (maximum instant burst size).
   */
  int capacity() default 10;

  /**
   * Tokens refilled per second.
   */
  double refillPerSecond() default 10;
}

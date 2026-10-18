package com.sun.base.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as audited and records the metadata the trail stores.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

  /**
   * Event label, e.g. HADES_CREATEANNOTATION.
   */
  String event();

  /**
   * Domain entity acted on, e.g. ReaderAnnotation.
   */
  String entity() default "";

  /**
   * Field names masked in the stored payload, on top of the secret deny-list.
   */
  String[] sensitiveFields() default {};

  /**
   * Entity fields snapshotted as prior state before an edit.
   */
  String[] beforeFields() default {};
}

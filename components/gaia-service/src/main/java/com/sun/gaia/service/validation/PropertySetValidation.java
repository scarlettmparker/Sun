package com.sun.gaia.service.validation;

import java.util.Map;

/**
 * Validates property-set values beyond the generic schema.
 */
public interface PropertySetValidation {

  /**
   * Returns true if this validator applies to the given set.
   *
   * @param ownerKey the owner key
   * @param propertySet the property set name
   * @return true if applicable
   */
  boolean supports(String ownerKey, String propertySet);

  /**
   * Validates the values for the set.
   *
   * @param values the values to validate
   */
  void validate(Map<String, Object> values);
}

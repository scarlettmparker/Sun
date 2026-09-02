package com.sun.gaia.service.validation;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Applies registered property-set validations.
 */
@Component
public class PropertySetValidationFactory {

  private final List<PropertySetValidation> validations;

  /**
   * Creates the factory with all registered validations.
   *
   * @param validations the validations
   */
  public PropertySetValidationFactory(List<PropertySetValidation> validations) {
    this.validations = validations;
  }

  /**
   * Runs all validators that support the given set.
   *
   * @param ownerKey the owner key
   * @param propertySet the property set name
   * @param values the values to validate
   */
  public void validate(String ownerKey, String propertySet, Map<String, Object> values) {
    for (PropertySetValidation validation : validations) {
      if (validation.supports(ownerKey, propertySet)) {
        validation.validate(values);
      }
    }
  }
}

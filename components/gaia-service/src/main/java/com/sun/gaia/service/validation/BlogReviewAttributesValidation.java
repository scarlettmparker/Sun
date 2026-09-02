package com.sun.gaia.service.validation;

import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Validates Blog review-attributes rating range.
 */
@Component
public class BlogReviewAttributesValidation implements PropertySetValidation {

  /**
   * Returns true for Blog review-attributes.
   *
   * @param ownerKey the owner key
   * @param propertySet the property set name
   * @return true if Blog review-attributes
   */
  @Override
  public boolean supports(String ownerKey, String propertySet) {
    return "Blog".equals(ownerKey) && "review-attributes".equals(propertySet);
  }

  /**
   * Validates rating is within 0 to ratingMax.
   *
   * @param values the values to validate
   */
  @Override
  public void validate(Map<String, Object> values) {
    Object ratingObj = values.get("rating");
    if (ratingObj == null) {
      return;
    }
    double max = 100;
    Object maxObj = values.get("ratingMax");
    if (maxObj instanceof Number number) {
      max = number.doubleValue();
    }
    double rating;
    if (ratingObj instanceof Number number) {
      rating = number.doubleValue();
    } else {
      return;
    }
    if (rating < 0 || rating > max) {
      throw new IllegalArgumentException("rating must be 0 to " + stripTrailingZero(max));
    }
  }

  /**
   * Formats max without trailing zero.
   *
   * @param value the value
   * @return formatted string
   */
  private String stripTrailingZero(double value) {
    if (value == Math.floor(value)) {
      return String.valueOf((long) value);
    }
    return String.valueOf(value);
  }
}

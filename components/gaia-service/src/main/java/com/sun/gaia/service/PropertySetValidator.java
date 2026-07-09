package com.sun.gaia.service;

import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates a values map against a property-set schema.
 */
@Component
public class PropertySetValidator {

  private static final Pattern HEX_COLOUR = Pattern.compile("^#[0-9a-fA-F]{3,8}$");

  /**
   * Validates the given values against the schema properties.
   *
   * @param schemaProperties the schema property definitions
   * @param values the values to validate
   */
  public void validate(Map<String, Object> schemaProperties, Map<String, Object> values) {
    if (schemaProperties == null) {
      return;
    }
    for (String key : values.keySet()) {
      if (!schemaProperties.containsKey(key)) {
        throw new IllegalArgumentException("Unknown property: " + key);
      }
    }
    for (Map.Entry<String, Object> entry : schemaProperties.entrySet()) {
      String key = entry.getKey();
      Map<String, Object> definition = asMap(entry.getValue());
      boolean required = Boolean.TRUE.equals(definition.get("required"));
      if (!values.containsKey(key)) {
        if (required) {
          throw new IllegalArgumentException("Missing required property: " + key);
        }
        continue;
      }
      String type = String.valueOf(definition.getOrDefault("type", "string"));
      checkType(key, type, values.get(key));
    }
  }

  /**
   * Checks a single value matches its declared type.
   *
   * @param key the property name
   * @param type the declared type
   * @param value the value to check
   */
  private void checkType(String key, String type, Object value) {
    switch (type) {
      case "color" -> {
        if (!(value instanceof String s) || !HEX_COLOUR.matcher(s).matches()) {
          throw new IllegalArgumentException("Property " + key + " must be a hex colour");
        }
      }
      case "string" -> {
        if (!(value instanceof String)) {
          throw new IllegalArgumentException("Property " + key + " must be a string");
        }
      }
      case "number" -> {
        if (!(value instanceof Number)) {
          throw new IllegalArgumentException("Property " + key + " must be a number");
        }
      }
      case "boolean" -> {
        if (!(value instanceof Boolean)) {
          throw new IllegalArgumentException("Property " + key + " must be a boolean");
        }
      }
      default -> {
        // Unknown schema types are left unconstrained.
      }
    }
  }

  /**
   * Coerces a schema definition value into a map.
   *
   * @param value the definition value
   * @return the definition as a map, or an empty map when not mappable
   */
  private Map<String, Object> asMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return coerceMap(map);
    }
    return Map.of();
  }

  /**
   * Converts a raw map into a string-keyed map.
   *
   * @param map the raw map
   * @return the coerced map
   */
  private Map<String, Object> coerceMap(Map<?, ?> map) {
    Map<String, Object> result = new java.util.LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      result.put(String.valueOf(entry.getKey()), entry.getValue());
    }
    return result;
  }
}

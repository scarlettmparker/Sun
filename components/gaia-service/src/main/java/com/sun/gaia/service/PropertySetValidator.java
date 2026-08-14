package com.sun.gaia.service;

import java.util.LinkedHashMap;
import java.util.List;
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
   * Schema key reserved for union groups.
   */
  private static final String ONEOF_KEY = "oneOf";

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
      if (ONEOF_KEY.equals(key) || !schemaProperties.containsKey(key)) {
        throw new IllegalArgumentException("Unknown property: " + key);
      }
    }
    for (Map.Entry<String, Object> entry : schemaProperties.entrySet()) {
      String key = entry.getKey();
      if (ONEOF_KEY.equals(key)) {
        continue;
      }
      Map<String, Object> definition = asMap(entry.getValue());
      boolean required = Boolean.TRUE.equals(definition.get("required"));
      if (!values.containsKey(key)) {
        if (required) {
          throw new IllegalArgumentException("Missing required property: " + key);
        }
        continue;
      }
      checkValue(key, definition, values.get(key));
    }
    validateOneOf(schemaProperties, values);
  }

  /**
   * Checks a single value against its definition.
   *
   * @param key the property name
   * @param definition the property definition
   * @param value the value to check
   */
  private void checkValue(String key, Map<String, Object> definition, Object value) {
    String type = String.valueOf(definition.getOrDefault("type", "string"));
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
      case "object" -> {
        if (!(value instanceof Map<?, ?> map)) {
          throw new IllegalArgumentException("Property " + key + " must be an object");
        }
        Map<String, Object> nested = asMap(definition.get("properties"));
        if (!nested.isEmpty()) {
          validate(nested, coerceMap(map));
        }
      }
      case "array" -> {
        if (!(value instanceof List<?> list)) {
          throw new IllegalArgumentException("Property " + key + " must be an array");
        }
        Map<String, Object> items = asMap(definition.get("items"));
        if (!items.isEmpty()) {
          for (Object element : list) {
            checkValue(key, items, element);
          }
        }
      }
      default -> {
        // Unknown schema types are left unconstrained.
      }
    }
  }

  /**
   * Enforces that exactly one key of every oneOf group is present.
   *
   * @param schemaProperties the schema property definitions
   * @param values the values to validate
   */
  private void validateOneOf(Map<String, Object> schemaProperties, Map<String, Object> values) {
    Object oneOfValue = schemaProperties.get(ONEOF_KEY);
    if (!(oneOfValue instanceof List<?> groups)) {
      return;
    }
    for (Object group : groups) {
      if (!(group instanceof List<?> keys)) {
        continue;
      }
      long present = keys.stream().filter(k -> values.containsKey(String.valueOf(k))).count();
      if (present != 1) {
        throw new IllegalArgumentException("Exactly one of " + keys + " must be present");
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
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      result.put(String.valueOf(entry.getKey()), entry.getValue());
    }
    return result;
  }
}

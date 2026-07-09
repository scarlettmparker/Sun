package com.sun.gaia.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertySetValidatorTest {

  private final PropertySetValidator validator = new PropertySetValidator();

  private Map<String, Object> schema(Map<String, Object>... definitions) {
    Map<String, Object> properties = new LinkedHashMap<>();
    for (Map<String, Object> definition : definitions) {
      properties.putAll(definition);
    }
    return properties;
  }

  private Map<String, Object> colour(String name, boolean required) {
    Map<String, Object> definition = new LinkedHashMap<>();
    definition.put("type", "color");
    definition.put("required", required);
    Map<String, Object> named = new LinkedHashMap<>();
    named.put(name, definition);
    return named;
  }

  @Test
  void validate_acceptsValuesMatchingTheSchema() {
    Map<String, Object> schema = schema(colour("primary", true), colour("primary-hover", false));
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("primary", "#d90429");
    values.put("primary-hover", "#fb3758");

    assertThatCode(() -> validator.validate(schema, values)).doesNotThrowAnyException();
  }

  @Test
  void validate_allowsMissingOptionalProperties() {
    Map<String, Object> schema = schema(colour("primary", true), colour("primary-hover", false));
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("primary", "#d90429");

    assertThatCode(() -> validator.validate(schema, values)).doesNotThrowAnyException();
  }

  @Test
  void validate_throwsWhenARequiredPropertyIsMissing() {
    Map<String, Object> schema = schema(colour("primary", true));
    Map<String, Object> values = new LinkedHashMap<>();

    assertThatThrownBy(() -> validator.validate(schema, values))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("primary");
  }

  @Test
  void validate_throwsOnAnUnknownProperty() {
    Map<String, Object> schema = schema(colour("primary", true));
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("primary", "#d90429");
    values.put("nonsense", "#000000");

    assertThatThrownBy(() -> validator.validate(schema, values))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nonsense");
  }

  @Test
  void validate_throwsOnANonHexColour() {
    Map<String, Object> schema = schema(colour("primary", true));
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("primary", "red");

    assertThatThrownBy(() -> validator.validate(schema, values))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("primary");
  }

  @Test
  void validate_isNoOpWhenNoSchemaIsProvided() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("anything", "goes");

    assertThatCode(() -> validator.validate(null, values)).doesNotThrowAnyException();
  }
}

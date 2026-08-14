package com.sun.gaia.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
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

  @Test
  void validate_acceptsNestedObjectValues() {
    Map<String, Object> nested = new LinkedHashMap<>();
    nested.put("type", "string");
    nested.put("required", true);
    Map<String, Object> nameDefinition = new LinkedHashMap<>();
    nameDefinition.put("name", nested);
    Map<String, Object> contentDefinition = new LinkedHashMap<>();
    contentDefinition.put("type", "object");
    contentDefinition.put("properties", nameDefinition);
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("content", contentDefinition);

    Map<String, Object> values = new LinkedHashMap<>();
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("name", "What is Language Transfer?");
    values.put("content", content);

    assertThatCode(() -> validator.validate(schema, values)).doesNotThrowAnyException();
  }

  @Test
  void validate_rejectsNonObjectForObjectType() {
    Map<String, Object> contentDefinition = new LinkedHashMap<>();
    contentDefinition.put("type", "object");
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("content", contentDefinition);

    Map<String, Object> values = new LinkedHashMap<>();
    values.put("content", "not an object");

    assertThatThrownBy(() -> validator.validate(schema, values))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("content");
  }

  @Test
  void validate_rejectsUnknownNestedKeys() {
    Map<String, Object> contentDefinition = new LinkedHashMap<>();
    contentDefinition.put("type", "object");
    contentDefinition.put("properties", Map.of("name", Map.of("type", "string")));
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("content", contentDefinition);

    Map<String, Object> values = new LinkedHashMap<>();
    values.put("content", Map.of("title", "unexpected"));

    assertThatThrownBy(() -> validator.validate(schema, values))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("title");
  }

  @Test
  void validate_rejectsMissingRequiredNestedKey() {
    Map<String, Object> contentDefinition = new LinkedHashMap<>();
    contentDefinition.put("type", "object");
    contentDefinition.put("properties",
        Map.of("name", Map.of("type", "string", "required", true)));
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("content", contentDefinition);

    Map<String, Object> values = new LinkedHashMap<>();
    values.put("content", Map.of());

    assertThatThrownBy(() -> validator.validate(schema, values))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test
  void validate_acceptsArrayValues() {
    Map<String, Object> wordsDefinition = new LinkedHashMap<>();
    wordsDefinition.put("type", "array");
    wordsDefinition.put("items", Map.of("type", "string"));
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("words", wordsDefinition);

    Map<String, Object> values = new LinkedHashMap<>();
    values.put("words", List.of("texts", "reader"));

    assertThatCode(() -> validator.validate(schema, values)).doesNotThrowAnyException();
  }

  @Test
  void validate_rejectsNonArrayForArrayType() {
    Map<String, Object> wordsDefinition = new LinkedHashMap<>();
    wordsDefinition.put("type", "array");
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("words", wordsDefinition);

    Map<String, Object> values = new LinkedHashMap<>();
    values.put("words", "not an array");

    assertThatThrownBy(() -> validator.validate(schema, values))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("words");
  }

  @Test
  void validate_rejectsWrongArrayElementType() {
    Map<String, Object> wordsDefinition = new LinkedHashMap<>();
    wordsDefinition.put("type", "array");
    wordsDefinition.put("items", Map.of("type", "string"));
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("words", wordsDefinition);

    Map<String, Object> values = new LinkedHashMap<>();
    values.put("words", List.of("texts", 5));

    assertThatThrownBy(() -> validator.validate(schema, values))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("words");
  }

  @Test
  void validate_acceptsExactlyOneOneOfKey() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("content", Map.of("type", "object"));
    schema.put("remoteObject", Map.of("type", "string"));
    schema.put("oneOf", List.of(List.of("content", "remoteObject")));

    Map<String, Object> values = new LinkedHashMap<>();
    values.put("remoteObject", "briareus:post:abc");

    assertThatCode(() -> validator.validate(schema, values)).doesNotThrowAnyException();
  }

  @Test
  void validate_rejectsOneOfWithNoKeysPresent() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("content", Map.of("type", "object"));
    schema.put("remoteObject", Map.of("type", "string"));
    schema.put("oneOf", List.of(List.of("content", "remoteObject")));

    Map<String, Object> values = new LinkedHashMap<>();

    assertThatThrownBy(() -> validator.validate(schema, values))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Exactly one of");
  }

  @Test
  void validate_rejectsOneOfWithMultipleKeysPresent() {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("content", Map.of("type", "object"));
    schema.put("remoteObject", Map.of("type", "string"));
    schema.put("oneOf", List.of(List.of("content", "remoteObject")));

    Map<String, Object> values = new LinkedHashMap<>();
    values.put("content", Map.of());
    values.put("remoteObject", "briareus:post:abc");

    assertThatThrownBy(() -> validator.validate(schema, values))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Exactly one of");
  }
}

package com.sun.base.audit.redaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PayloadRedactorTest {

  private PayloadRedactor redactor;

  @BeforeEach
  void setUp() {
    redactor = new PayloadRedactor(new ObjectMapper());
  }

  @Test
  void redact_masksSecretFragmentsCaseInsensitively() {
    Map<String, Object> payload = Map.of(
        "password", "hunter2",
        "ApiKeyValue", "abc",
        "refreshToken", "xyz");

    JsonNode redacted = redactor.redact(payload, Set.of());

    assertThat(redacted.get("password").asText()).isEqualTo(PayloadRedactor.MASK);
    assertThat(redacted.get("ApiKeyValue").asText()).isEqualTo(PayloadRedactor.MASK);
    assertThat(redacted.get("refreshToken").asText()).isEqualTo(PayloadRedactor.MASK);
  }

  @Test
  void redact_masksDeclaredSensitiveFields() {
    Map<String, Object> payload = Map.of("email", "a@b.com", "body", "hello");

    JsonNode redacted = redactor.redact(payload, Set.of("email"));

    assertThat(redacted.get("email").asText()).isEqualTo(PayloadRedactor.MASK);
    assertThat(redacted.get("body").asText()).isEqualTo("hello");
  }

  @Test
  void redact_descendsIntoNestedObjectsAndArrays() {
    Map<String, Object> payload = Map.of(
        "input", Map.of("token", "secret"),
        "items", java.util.List.of(Map.of("authorization", "bearer")));

    JsonNode redacted = redactor.redact(payload, Set.of());

    assertThat(redacted.get("input").get("token").asText()).isEqualTo(PayloadRedactor.MASK);
    assertThat(redacted.get("items").get(0).get("authorization").asText()).isEqualTo(PayloadRedactor.MASK);
  }

  @Test
  void redact_leavesInputUnchanged() {
    Map<String, Object> payload = new java.util.HashMap<>(Map.of("password", "hunter2"));

    redactor.redact(payload, Set.of());

    assertThat(payload.get("password")).isEqualTo("hunter2");
  }

  @Test
  void redact_returnsNullNodeForNullPayload() {
    JsonNode redacted = redactor.redact(null, Set.of());

    assertThat(redacted.isNull()).isTrue();
  }
}

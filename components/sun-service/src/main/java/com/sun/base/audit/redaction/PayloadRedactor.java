package com.sun.base.audit.redaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Masks sensitive fields in a payload before it's stored. Passwords and tokens
 * are always masked.
 */
@Component
public class PayloadRedactor {

  /** Mask written in place of any redacted value. */
  public static final String MASK = "REDACTED";

  /**
   * Field-name fragments always treated as secrets and masked regardless of
   * operation. Matched case-insensitively on contains, so password,
   * newPassword, and passwordHash are all caught.
   */
  private static final Set<String> SECRET_FRAGMENTS =
      Set.of("password", "secret", "token", "authorization", "credential", "apikey", "api_key");

  private final ObjectMapper mapper;

  public PayloadRedactor(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Produces a deep-copied, masked view of the payload. Secrets and sensitive
   * fields are replaced with a mask. The input is not mutated.
   *
   * @param payload the request variables/body to mask
   * @param sensitiveFields field names to mask in addition to the deny-list
   * @return a new masked JSON tree
   */
  public JsonNode redact(Object payload, Set<String> sensitiveFields) {
    JsonNode root = mapper.valueToTree(payload);
    redactInPlace(root, sensitiveFields);
    return root;
  }

  private void redactInPlace(JsonNode node, Set<String> sensitiveFields) {
    if (node == null || !node.isObject()) {
      return;
    }
    ObjectNode obj = (ObjectNode) node;
    obj.fieldNames().forEachRemaining(name -> {
      if (isSecret(name) || contains(sensitiveFields, name)) {
        obj.set(name, TextNode.valueOf(MASK));
      } else {
        JsonNode child = obj.get(name);
        if (child != null && child.isObject()) {
          redactInPlace(child, sensitiveFields);
        } else if (child != null && child.isArray()) {
          redactArray((ArrayNode) child, sensitiveFields);
        }
      }
    });
  }

  private void redactArray(ArrayNode array, Set<String> sensitiveFields) {
    for (JsonNode element : array) {
      if (element.isObject()) {
        redactInPlace(element, sensitiveFields);
      }
    }
  }

  private boolean isSecret(String fieldName) {
    String lower = fieldName.toLowerCase();
    return SECRET_FRAGMENTS.stream().anyMatch(lower::contains);
  }

  private boolean contains(Set<String> fields, String fieldName) {
    if (fields == null || fields.isEmpty()) {
      return false;
    }
    return fields.stream().anyMatch(f -> f.equalsIgnoreCase(fieldName));
  }
}

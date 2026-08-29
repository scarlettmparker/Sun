package com.sun.base.permify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Shared Permify HTTP client for check and write.
 */
@Component
public class PermifyClient {

  private static final Logger logger = LoggerFactory.getLogger(PermifyClient.class);

  private final PermifyProps props;
  private final RestClient restClient;

  public PermifyClient(PermifyProps props) {
    this.props = props;
    this.restClient = RestClient.builder().baseUrl(props.getHttpEndpoint()).build();
  }

  /**
   * Checks whether a subject may perform an action on an object.
   *
   * @param subject the subject e.g. user:uuid
   * @param action the action e.g. view
   * @param object the object e.g. private_note:uuid
   * @return true when permitted
   */
  public boolean check(String subject, String action, String object) {
    if (!props.isEnabled()) {
      return false;
    }
    try {
      String[] subjectParts = subject.split(":", 2);
      String[] objectParts = object.split(":", 2);
      if (subjectParts.length != 2 || objectParts.length != 2) {
        return false;
      }
      Map<String, Object> body = new HashMap<>();
      body.put("metadata", Map.of("schemaVersion", "", "depth", 8));
      body.put("entity", Map.of("type", objectParts[0], "id", objectParts[1]));
      body.put("permission", action);
      body.put("subject", Map.of("type", subjectParts[0], "id", subjectParts[1]));
      Map response = restClient.post()
          .uri("/v1/tenants/t1/permissions/check")
          .body(body)
          .retrieve()
          .body(Map.class);
      return response != null && "CHECK_RESULT_ALLOWED".equals(response.get("can"));
    } catch (Exception e) {
      logger.warn("Permify check failed for {} {} {}", subject, action, object, e);
      return false;
    }
  }

  /**
   * Writes a single relation tuple.
   *
   * @param object the object e.g. private_note:uuid
   * @param relation the relation e.g. viewer
   * @param subject the subject e.g. user:uuid
   */
  public void writeTuple(String object, String relation, String subject) {
    if (!props.isEnabled()) {
      return;
    }
    try {
      String[] subjectParts = subject.split(":", 2);
      String[] objectParts = object.split(":", 2);
      if (subjectParts.length != 2 || objectParts.length != 2) {
        return;
      }
      Map<String, Object> tuple = new HashMap<>();
      tuple.put("entity", Map.of("type", objectParts[0], "id", objectParts[1]));
      tuple.put("relation", relation);
      tuple.put("subject", Map.of("type", subjectParts[0], "id", subjectParts[1]));
      Map<String, Object> body = new HashMap<>();
      body.put("metadata", Map.of("schemaVersion", ""));
      body.put("tuples", List.of(tuple));
      restClient.post()
          .uri("/v1/tenants/t1/relationships/write")
          .body(body)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      logger.warn("Permify write failed for {} {} {}", object, relation, subject, e);
      throw new RuntimeException("Permify write failed", e);
    }
  }

  /**
   * Writes multiple relation tuples.
   *
   * @param tuples the tuples
   */
  public void writeTuples(List<Map<String, String>> tuples) {
    if (!props.isEnabled() || tuples == null || tuples.isEmpty()) {
      return;
    }
    try {
      List<Map<String, Object>> permifyTuples = new ArrayList<>();
      for (Map<String, String> t : tuples) {
        String object = t.get("object");
        String relation = t.get("relation");
        String subject = t.get("subject");
        if (object == null || relation == null || subject == null) {
          continue;
        }
        String[] subjectParts = subject.split(":", 2);
        String[] objectParts = object.split(":", 2);
        if (subjectParts.length != 2 || objectParts.length != 2) {
          continue;
        }
        Map<String, Object> tuple = new HashMap<>();
        tuple.put("entity", Map.of("type", objectParts[0], "id", objectParts[1]));
        tuple.put("relation", relation);
        tuple.put("subject", Map.of("type", subjectParts[0], "id", subjectParts[1]));
        permifyTuples.add(tuple);
      }
      if (permifyTuples.isEmpty()) {
        return;
      }
      Map<String, Object> body = new HashMap<>();
      body.put("metadata", Map.of("schemaVersion", ""));
      body.put("tuples", permifyTuples);
      restClient.post()
          .uri("/v1/tenants/t1/relationships/write")
          .body(body)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      logger.warn("Permify batch write failed for {} tuples", tuples.size(), e);
      throw new RuntimeException("Permify batch write failed", e);
    }
  }
}

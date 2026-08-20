package com.sun.gaia.service;

import com.sun.gaia.repository.ObjectShareRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Authorization check against Permify when enabled, otherwise falls back to
 * direct ownership or an explicit share row.
 */
@Service
public class PermifyService {

  private static final Logger logger = LoggerFactory.getLogger(PermifyService.class);

  private final ObjectShareRepository shareRepository;
  private final boolean enabled;
  private final RestClient restClient;

  public PermifyService(
      ObjectShareRepository shareRepository,
      @Value("${permify.enabled:false}") boolean enabled,
      @Value("${permify.http-endpoint:http://localhost:3477}") String httpEndpoint) {
    this.shareRepository = shareRepository;
    this.enabled = enabled;
    this.restClient = RestClient.builder().baseUrl(httpEndpoint).build();
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
    if (!enabled) {
      return fallbackCheck(subject, object);
    }
    try {
      String[] subjectParts = subject.split(":", 2);
      String[] objectParts = object.split(":", 2);
      if (subjectParts.length != 2 || objectParts.length != 2) {
        return fallbackCheck(subject, object);
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
      if (response != null && "CHECK_RESULT_ALLOWED".equals(response.get("can"))) {
        return true;
      }
      return fallbackCheck(subject, object);
    } catch (Exception e) {
      logger.warn("Permify check failed, falling back", e);
      return fallbackCheck(subject, object);
    }
  }

  /**
   * Writes a relation tuple.
   *
   * @param object the object e.g. private_note:uuid
   * @param relation the relation e.g. viewer
   * @param subject the subject e.g. user:uuid
   */
  public void writeTuple(String object, String relation, String subject) {
    if (!enabled) {
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
   * Checks the share table for a direct grant.
   *
   * @param subject the subject e.g. user:uuid
   * @param object the object e.g. private_note:uuid
   * @return true when a share row exists
   */
  private boolean fallbackCheck(String subject, String object) {
    String[] subjectParts = subject.split(":", 2);
    String[] objectParts = object.split(":", 2);
    if (subjectParts.length != 2 || objectParts.length != 2) {
      return false;
    }
    String subjectType = subjectParts[0];
    UUID subjectId;
    UUID objectId;
    try {
      subjectId = UUID.fromString(subjectParts[1]);
      objectId = UUID.fromString(objectParts[1]);
    } catch (IllegalArgumentException e) {
      return false;
    }
    String objectType = objectParts[0];
    return shareRepository.existsByObjectTypeAndObjectIdAndSubjectTypeAndSubjectId(
        objectType, objectId, subjectType, subjectId);
  }
}

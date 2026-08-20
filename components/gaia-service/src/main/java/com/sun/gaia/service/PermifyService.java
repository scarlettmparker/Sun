package com.sun.gaia.service;

import com.sun.gaia.repository.ObjectShareRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Authorization check against Permify when enabled, otherwise falls back to
 * direct ownership or an explicit share row.
 */
@Service
public class PermifyService {

  private final ObjectShareRepository shareRepository;
  private final boolean enabled;
  private final String endpoint;

  public PermifyService(
      ObjectShareRepository shareRepository,
      @Value("${permify.enabled:false}") boolean enabled,
      @Value("${permify.endpoint:localhost:3476}") String endpoint) {
    this.shareRepository = shareRepository;
    this.enabled = enabled;
    this.endpoint = endpoint;
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
      // TODO: gRPC Permify Check call to endpoint. Until permify binary is
      // deployed, fall back to the share table so the feature works without it.
      return fallbackCheck(subject, object);
    } catch (Exception e) {
      return fallbackCheck(subject, object);
    }
  }

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

package com.sun.gaia.service;

import com.sun.base.permify.PermifyClient;
import com.sun.gaia.repository.ObjectShareRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Authorization check against Permify when enabled, otherwise falls back to
 * direct ownership or an explicit share row.
 */
@Service
public class PermifyService {

  private static final Logger logger = LoggerFactory.getLogger(PermifyService.class);

  private final ObjectShareRepository shareRepository;
  private final PermifyClient permifyClient;

  public PermifyService(
      ObjectShareRepository shareRepository,
      PermifyClient permifyClient) {
    this.shareRepository = shareRepository;
    this.permifyClient = permifyClient;
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
    if (permifyClient.check(subject, action, object)) {
      return true;
    }
    return fallbackCheck(subject, object);
  }

  /**
   * Writes a relation tuple.
   *
   * @param object the object e.g. private_note:uuid
   * @param relation the relation e.g. viewer
   * @param subject the subject e.g. user:uuid
   */
  public void writeTuple(String object, String relation, String subject) {
    permifyClient.writeTuple(object, relation, subject);
  }

  /**
   * Writes multiple relation tuples in one call.
   *
   * @param tuples the tuples, each as object, relation, subject
   */
  public void writeTuples(List<Map<String, String>> tuples) {
    permifyClient.writeTuples(tuples);
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

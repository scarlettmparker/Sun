package com.sun.base.permify;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Static helpers for Permify subject/object strings and ownership checks.
 */
public final class PermifyUtil {

  private PermifyUtil() {
  }

  /**
   * Builds a subject string.
   *
   * @param type the subject type
   * @param id the subject id
   * @return the subject string
   */
  public static String subject(String type, String id) {
    return type + ":" + id;
  }

  /**
   * Builds a subject string for a user.
   *
   * @param userId the user id
   * @return the subject string
   */
  public static String userSubject(UUID userId) {
    return "user:" + userId;
  }

  /**
   * Builds an object string.
   *
   * @param type the object type
   * @param id the object id
   * @return the object string
   */
  public static String object(String type, String id) {
    return type + ":" + id;
  }

  /**
   * Builds an object string for a UUID.
   *
   * @param type the object type
   * @param id the object id
   * @return the object string
   */
  public static String object(String type, UUID id) {
    return type + ":" + id;
  }

  /**
   * Builds a tuple map for Permify writes.
   *
   * @param object the object
   * @param relation the relation
   * @param subject the subject
   * @return the tuple map
   */
  public static Map<String, String> tuple(String object, String relation, String subject) {
    Map<String, String> m = new HashMap<>();
    m.put("object", object);
    m.put("relation", relation);
    m.put("subject", subject);
    return m;
  }

  /**
   * Checks ownership or permify view permission.
   *
   * @param viewer the viewer id
   * @param ownerId the owner id
   * @param client the permify client
   * @param object the object string for permify check
   * @return true when visible
   */
  public static boolean canView(UUID viewer, UUID ownerId, PermifyClient client, String object) {
    if (viewer != null && viewer.equals(ownerId)) {
      return true;
    }
    return viewer != null && client.check(userSubject(viewer), "view", object);
  }
}

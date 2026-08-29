package com.sun.briareus.service;

import com.sun.briareus.model.PostEntity;
import com.sun.gaia.model.ObjectShareEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * DB predicate for blog visibility.
 */
public final class BriareusVisibilitySpec {

  private BriareusVisibilitySpec() {
  }

  /**
   * Builds visibility predicate for the given viewer.
   *
   * @param viewer the viewer id
   * @return the spec
   */
  public static Specification<PostEntity> visibleTo(UUID viewer) {
    return (root, query, cb) -> {
      if (viewer == null) {
        return cb.isTrue(root.get("type").get("name").in("BOT_FAQ", "BOT_HELP"));
      }
      Predicate isPublic = root.get("type").get("name").in("BOT_FAQ", "BOT_HELP");
      Predicate isOwner = cb.equal(root.get("createdBy"), viewer);
      Subquery<Long> sq = query.subquery(Long.class);
      Root<ObjectShareEntity> s = sq.from(ObjectShareEntity.class);
      sq.select(cb.literal(1L)).where(
          cb.equal(s.get("objectType"), "briareus_post"),
          cb.equal(s.get("objectId"), root.get("id")),
          cb.equal(s.get("subjectType"), "user"),
          cb.equal(s.get("subjectId"), viewer),
          cb.equal(s.get("relation"), "VIEWER"));
      return cb.or(isPublic, isOwner, cb.exists(sq));
    };
  }
}

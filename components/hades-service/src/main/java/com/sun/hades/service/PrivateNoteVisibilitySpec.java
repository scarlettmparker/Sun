package com.sun.hades.service;

import com.sun.gaia.model.ObjectShareEntity;
import com.sun.hades.model.PrivateNoteEntity;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * DB predicate for private note visibility.
 */
public final class PrivateNoteVisibilitySpec {

  private PrivateNoteVisibilitySpec() {
  }

  /**
   * Builds visibility predicate for the given viewer.
   *
   * @param viewer the viewer id
   * @return the spec
   */
  public static Specification<PrivateNoteEntity> visibleTo(UUID viewer) {
    return (root, query, cb) -> {
      if (viewer == null) {
        return cb.disjunction();
      }
      Subquery<Long> sq = query.subquery(Long.class);
      Root<ObjectShareEntity> s = sq.from(ObjectShareEntity.class);
      sq.select(cb.literal(1L)).where(
          cb.equal(s.get("objectType"), "private_note"),
          cb.equal(s.get("objectId"), root.get("id")),
          cb.equal(s.get("subjectType"), "user"),
          cb.equal(s.get("subjectId"), viewer),
          cb.equal(s.get("relation"), "VIEWER"));
      return cb.or(
          cb.equal(root.get("ownerId"), viewer),
          cb.exists(sq));
    };
  }
}

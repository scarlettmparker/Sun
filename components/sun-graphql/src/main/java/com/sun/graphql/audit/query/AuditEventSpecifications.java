package com.sun.graphql.audit.query;

import com.sun.base.audit.entity.AuditEvent;
import com.sun.graphql.codegen.types.AuditEventFilterInput;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds a JPA specification from an audit filter input.
 */
public final class AuditEventSpecifications {

  private AuditEventSpecifications() {
  }

  /**
   * Builds the combined specification for the optional filters.
   *
   * @param filter the optional filters, or null
   * @return the specification
   */
  public static Specification<AuditEvent> of(AuditEventFilterInput filter) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (filter != null) {
        addEquals(predicates, cb, root, "eventType", filter.getEventType());
        addIn(predicates, root, "eventType", filter.getEventTypes());
        addEnum(predicates, root, "outcome", filter.getOutcome());
        addEnumList(predicates, root, "outcome", filter.getOutcomes());
        addUuid(predicates, cb, root, "userId", filter.getUserId());
        addUuidList(predicates, root, "userId", filter.getUserIds());
        addEquals(predicates, cb, root, "targetEntity", filter.getTargetEntity());
        addUuid(predicates, cb, root, "targetEntityId", filter.getTargetEntityId());
        addEquals(predicates, cb, root, "namespace", filter.getNamespace());
        addEmailSearch(predicates, cb, root, filter.getSearch());
        addRange(predicates, cb, root, filter.getFrom(), filter.getTo());
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static void addEquals(List<Predicate> predicates, CriteriaBuilder cb,
                                Root<AuditEvent> root, String field, String value) {
    if (value != null) {
      predicates.add(cb.equal(root.get(field), value));
    }
  }

  private static void addIn(List<Predicate> predicates, Root<AuditEvent> root,
                            String field, List<String> values) {
    if (values != null && !values.isEmpty()) {
      predicates.add(root.get(field).in(values));
    }
  }

  private static void addEnum(List<Predicate> predicates, Root<AuditEvent> root,
                              String field, Object value) {
    if (value != null) {
      predicates.add(root.get(field).in(value.toString()));
    }
  }

  private static void addEnumList(List<Predicate> predicates, Root<AuditEvent> root,
                                  String field, List<?> values) {
    if (values != null && !values.isEmpty()) {
      predicates.add(root.get(field).in(values.stream().map(Object::toString).toList()));
    }
  }

  private static void addUuid(List<Predicate> predicates, CriteriaBuilder cb,
                              Root<AuditEvent> root, String field, String value) {
    if (value != null && !value.isBlank()) {
      predicates.add(cb.equal(root.get(field), java.util.UUID.fromString(value)));
    }
  }

  private static void addUuidList(List<Predicate> predicates, Root<AuditEvent> root,
                                  String field, List<String> values) {
    if (values != null && !values.isEmpty()) {
      predicates.add(root.get(field).in(values.stream().map(java.util.UUID::fromString).toList()));
    }
  }

  private static void addEmailSearch(List<Predicate> predicates, CriteriaBuilder cb,
                                     Root<AuditEvent> root, String search) {
    if (search != null && !search.isBlank()) {
      predicates.add(cb.like(cb.lower(root.get("userName")), "%" + search.toLowerCase() + "%"));
    }
  }

  private static void addRange(List<Predicate> predicates, CriteriaBuilder cb,
                               Root<AuditEvent> root, OffsetDateTime from, OffsetDateTime to) {
    if (from != null) {
      predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.toInstant()));
    }
    if (to != null) {
      predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to.toInstant()));
    }
  }
}

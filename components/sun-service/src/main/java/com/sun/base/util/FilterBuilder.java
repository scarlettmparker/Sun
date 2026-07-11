package com.sun.base.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Converts a list of {@link FilterSpec} into a JPA {@link Specification}.
 */
public final class FilterBuilder {

  private FilterBuilder() {
  }

  /**
   * Builds a combined Specification from the given filters (AND-chained).
   *
   * @param filters the filter criteria
   * @return the specification, or null when filters is null or empty
   * @param <T> the entity type
   */
  public static <T> Specification<T> buildFilters(List<FilterSpec> filters) {
    if (filters == null || filters.isEmpty()) {
      return null;
    }
    Specification<T> result = null;
    for (FilterSpec filter : filters) {
      Specification<T> spec = toSpecification(filter);
      result = result == null ? spec : result.and(spec);
    }
    return result;
  }

  /**
   * Converts a single filter into a Specification.
   *
   * @param filter the filter criterion
   * @return the specification
   * @param <T> the entity type
   */
  private static <T> Specification<T> toSpecification(FilterSpec filter) {
    return (Root<T> root, jakarta.persistence.criteria.CriteriaQuery<?> query, CriteriaBuilder cb) -> {
      Path<?> path = resolvePath(root, filter.field());
      String value = filter.value();
      switch (filter.operator()) {
        case "EQUALS":
          return cb.equal(path, value);
        case "NOT_EQUALS":
          return cb.notEqual(path, value);
        case "MATCHES":
          return cb.like(cb.lower(path.as(String.class)), "%" + value.toLowerCase() + "%");
        case "STARTS_WITH":
          return cb.like(cb.lower(path.as(String.class)), value.toLowerCase() + "%");
        case "ENDS_WITH":
          return cb.like(cb.lower(path.as(String.class)), "%" + value.toLowerCase());
        case "GREATER_THAN":
          return cb.greaterThan(path.as(String.class), value);
        case "LESS_THAN":
          return cb.lessThan(path.as(String.class), value);
        case "GREATER_THAN_OR_EQUAL":
          return cb.greaterThanOrEqualTo(path.as(String.class), value);
        case "LESS_THAN_OR_EQUAL":
          return cb.lessThanOrEqualTo(path.as(String.class), value);
        case "IN":
          return path.in(Arrays.asList(value.split(",")));
        default:
          throw new IllegalArgumentException("Unknown filter operator: " + filter.operator());
      }
    };
  }

  /**
   * Resolves a dot-separated field path to a JPA Path, traversing nested joins.
   *
   * @param root the query root
   * @param field the dot-separated field path
   * @return the resolved path
   */
  private static Path<?> resolvePath(Root<?> root, String field) {
    String[] parts = field.split("\\.");
    Path<?> path = root.get(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      path = path.get(parts[i]);
    }
    return path;
  }
}

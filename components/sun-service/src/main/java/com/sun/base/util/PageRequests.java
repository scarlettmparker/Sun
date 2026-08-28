package com.sun.base.util;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds pageables from pagination fields shared by every GraphQL component.
 */
public final class PageRequests {

  private PageRequests() {
  }

  /**
   * Builds a pageable, falling back to the given defaults.
   *
   * @param page the zero-based page, or null for 0
   * @param size the page size, or null for unlimited (all results)
   * @param sortBy the sort property, or null for defaultSortBy
   * @param sortDir ASC or DESC, or null for defaultDir
   * @param defaultSortBy the property to sort by when sortBy is null
   * @param defaultDir the direction when sortDir is null
   * @return the pageable
   */
  public static Pageable of(Integer page, Integer size, String sortBy, String sortDir,
      String defaultSortBy, Sort.Direction defaultDir) {
    int p = page == null ? 0 : page;
    int s = size == null ? Integer.MAX_VALUE : size;
    String property = sortBy == null ? defaultSortBy : sortBy;
    Sort.Direction direction = (sortDir != null && sortDir.equalsIgnoreCase("DESC"))
        ? Sort.Direction.DESC
        : (sortDir != null && sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : defaultDir);
    return PageRequest.of(p, s, Sort.by(direction, property));
  }

  /**
   * Builds a pageable from an ordered list of sorts, falling back to defaults.
   *
   * @param page the zero-based page, or null for 0
   * @param size the page size, or null for unlimited (all results)
   * @param orders the ordered sort definitions, or null/empty for default
   * @param defaultSortBy the property to sort by when orders is null or empty
   * @param defaultDir the direction when orders is null or empty
   * @return the pageable
   */
  public static Pageable of(Integer page, Integer size, List<Sort.Order> orders,
      String defaultSortBy, Sort.Direction defaultDir) {
    int p = page == null ? 0 : page;
    int s = size == null ? Integer.MAX_VALUE : size;
    Sort sort = (orders == null || orders.isEmpty())
        ? Sort.by(defaultDir, defaultSortBy)
        : Sort.by(orders);
    return PageRequest.of(p, s, sort);
  }
}

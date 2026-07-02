package com.sun.base.util;

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
   * @param size the page size, or null for 10
   * @param sortBy the sort property, or null for defaultSortBy
   * @param sortDir ASC or DESC, or null for defaultDir
   * @param defaultSortBy the property to sort by when sortBy is null
   * @param defaultDir the direction when sortDir is null
   * @return the pageable
   */
  public static Pageable of(Integer page, Integer size, String sortBy, String sortDir,
      String defaultSortBy, Sort.Direction defaultDir) {
    int p = page != null ? page : 0;
    int s = size != null ? size : 10;
    String property = sortBy != null ? sortBy : defaultSortBy;
    Sort.Direction direction = (sortDir != null && sortDir.equalsIgnoreCase("DESC"))
        ? Sort.Direction.DESC
        : (sortDir != null && sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : defaultDir);
    return PageRequest.of(p, s, Sort.by(direction, property));
  }
}

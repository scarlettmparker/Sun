package com.sun.gaia.graphql.services.support;

import com.sun.base.util.PageRequests;
import com.sun.gaia.codegen.types.PageInfo;
import com.sun.gaia.codegen.types.PaginationInput;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Shared helpers for Gaia GraphQL services.
 */
public final class GaiaGraphQLSupport {

  private GaiaGraphQLSupport() {}

  /**
   * Converts a GraphQL PaginationInput into a Spring Pageable.
   *
   * @param pagination the pagination input
   * @param defaultSortBy the fallback sort field
   * @param defaultDir the fallback sort direction
   * @return the pageable
   */
  public static Pageable toPageable(PaginationInput pagination, String defaultSortBy,
      Sort.Direction defaultDir) {
    if (pagination == null) {
      return PageRequests.of(null, null, null, null, defaultSortBy, defaultDir);
    }
    return PageRequests.of(
        pagination.getPage(), pagination.getSize(),
        pagination.getSortBy(),
        pagination.getSortDir() == null ? null : pagination.getSortDir().name(),
        defaultSortBy, defaultDir);
  }

  /**
   * Converts a Spring Data page into GraphQL PageInfo.
   *
   * @param page the page
   * @return the page info
   */
  public static PageInfo toPageInfo(Page<?> page) {
    return PageInfo.newBuilder()
        .page(page.getNumber())
        .size(page.getSize())
        .totalPages(page.getTotalPages())
        .totalCount((int) page.getTotalElements())
        .hasNextPage(page.hasNext())
        .hasPreviousPage(page.hasPrevious())
        .build();
  }

  /**
   * Coerces a JSON input value into a string-keyed map.
   *
   * @param value the JSON value
   * @return the coerced map
   */
  public static Map<String, Object> asMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        result.put(String.valueOf(entry.getKey()), entry.getValue());
      }
      return result;
    }
    throw new IllegalArgumentException("Expected a JSON object");
  }

  /**
   * Returns the calling app's base URL for emailed links, or the fallback.
   *
   * @param appBaseUrl the fallback base URL
   * @return the app base URL
   */
  public static String resolveBaseUrl(String appBaseUrl) {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs != null) {
        String forwarded = attrs.getRequest().getHeader("X-App-Base-Url");
        if (forwarded != null && !forwarded.isBlank()) {
          return forwarded;
        }
      }
    } catch (Exception e) {
      // No servlet context available (e.g. test)
    }
    return appBaseUrl;
  }
}

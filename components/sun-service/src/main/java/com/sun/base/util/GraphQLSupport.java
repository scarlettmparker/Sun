package com.sun.base.util;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Shared utilities for GraphQL service methods across all Sun components.
 */
public final class GraphQLSupport {

  private GraphQLSupport() {
  }

  /**
   * Converts nullable pagination fields into a Pageable, applying defaults.
   *
   * @param page the page number (nullable)
   * @param size the page size (nullable)
   * @param sortBy the sort field (nullable)
   * @param sortDirName the sort direction name ("ASC" or "DESC", nullable)
   * @param defaultSortBy the default sort field
   * @param defaultDir the default sort direction
   * @return the pageable
   */
  public static Pageable toPageable(
      Integer page, Integer size, String sortBy, String sortDirName,
      String defaultSortBy, Sort.Direction defaultDir) {
    return PageRequests.of(page, size, sortBy, sortDirName, defaultSortBy, defaultDir);
  }

  /**
   * Extracts page metadata from a Spring data page.
   *
   * @param result the data page
   * @return the page metadata
   */
  public static PageMetadata pageMetadata(Page<?> result) {
    return new PageMetadata(
        result.getNumber(), result.getSize(), result.getTotalPages(),
        result.getTotalElements(), result.hasNext(), result.hasPrevious());
  }

  /**
   * Converts a list of items into filter specs using the provided mapper.
   *
   * @param items the raw items (e.g. codegen FilterInput objects)
   * @param mapper converts each item to a FilterSpec
   * @return the filter specs, or null when null or empty
   * @param <T> the item type
   */
  public static <T> List<FilterSpec> toFilterSpecs(
      List<T> items, Function<T, FilterSpec> mapper) {
    if (items == null || items.isEmpty()) {
      return null;
    }
    return items.stream().map(mapper).collect(Collectors.toList());
  }

  /**
   * Executes a mutation action, catching exceptions and returning the outcome.
   *
   * @param action the action to execute (returns the created/updated entity id)
   * @return the mutation outcome
   */
  public static MutationOutcome mutate(Callable<UUID> action) {
    try {
      UUID id = action.call();
      return new MutationOutcome(true, id != null ? id.toString() : null, "OK");
    } catch (IllegalArgumentException e) {
      return new MutationOutcome(false, null, e.getMessage());
    } catch (Exception e) {
      return new MutationOutcome(false, null, "Internal server error");
    }
  }

  /**
   * Page metadata fields, independent of any GraphQL codegen type.
   *
   * @param page the current page number
   * @param size the page size
   * @param totalPages the total number of pages
   * @param totalCount the total element count
   * @param hasNextPage whether a next page exists
   * @param hasPreviousPage whether a previous page exists
   */
  public record PageMetadata(
      int page, int size, int totalPages, long totalCount,
      boolean hasNextPage, boolean hasPreviousPage) {
  }

  /**
   * Mutation result, independent of any GraphQL codegen type.
   *
   * @param success whether the mutation succeeded
   * @param id the created/updated entity id (null on failure)
   * @param message the result message
   */
  public record MutationOutcome(boolean success, String id, String message) {
  }
}

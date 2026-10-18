package com.sun.graphql.audit.service;

import com.sun.base.audit.entity.AuditEvent;
import com.sun.base.audit.repository.AuditEventRepository;
import com.sun.base.util.PageRequests;
import com.sun.graphql.audit.query.AuditEventMapper;
import com.sun.graphql.audit.query.AuditEventSpecifications;
import com.sun.graphql.codegen.types.AuditEventFilterInput;
import com.sun.graphql.codegen.types.AuditFacets;
import com.sun.graphql.codegen.types.AuditPage;
import com.sun.graphql.codegen.types.PagedAuditEvents;
import com.sun.graphql.codegen.types.PaginationInput;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Read side of the audit trail.
 */
@Service
public class AuditQueryService {

  private static final Set<String> SORTABLE = Set.of(
      "createdAt", "eventType", "outcome", "targetEntity", "userName", "endpoint", "durationMs");

  private final AuditEventRepository repository;

  public AuditQueryService(AuditEventRepository repository) {
    this.repository = repository;
  }

  /**
   * Loads the audit trail page with its facets.
   *
   * @param pagination the pagination input, or null
   * @param filter the optional filters, or null
   * @return the paged events and facets
   */
  public AuditPage page(PaginationInput pagination, AuditEventFilterInput filter) {
    Pageable pageable = toPageable(pagination);
    Page<AuditEvent> events = repository.findAll(AuditEventSpecifications.of(filter), pageable);
    String namespace = filter == null ? null : filter.getNamespace();
    return AuditPage.newBuilder()
        .events(PagedAuditEvents.newBuilder()
            .items(events.getContent().stream().map(AuditEventMapper::toGraphQl).toList())
            .pageInfo(AuditEventMapper.toPageInfo(events))
            .build())
        .facets(facets(namespace))
        .build();
  }

  /**
   * Locates a single audit event.
   *
   * @param id the audit event id
   * @return the event, or empty when absent or malformed
   */
  public Optional<com.sun.graphql.codegen.types.AuditEvent> locate(String id) {
    UUID parsed = parseUuid(id);
    if (parsed == null) {
      return Optional.empty();
    }
    return repository.findById(parsed).map(AuditEventMapper::toGraphQl);
  }

  /**
   * Locates every audit event sharing a correlation id.
   *
   * @param correlationId the request correlation id
   * @return matching events
   */
  public List<com.sun.graphql.codegen.types.AuditEvent> byCorrelation(String correlationId) {
    UUID parsed = parseUuid(correlationId);
    if (parsed == null) {
      return List.of();
    }
    return repository.findByCorrelationId(parsed).stream().map(AuditEventMapper::toGraphQl).toList();
  }

  /**
   * Counts the selectable filter values for a namespace.
   *
   * @param namespace the namespace to scope to, or null for all
   * @return the facet values with counts
   */
  private AuditFacets facets(String namespace) {
    return AuditFacets.newBuilder()
        .eventTypes(AuditEventMapper.toFacetValues(repository.countByEventType(namespace)))
        .outcomes(AuditEventMapper.toFacetValues(repository.countByOutcome(namespace)))
        .actors(AuditEventMapper.toFacetValues(repository.countByUser(namespace)))
        .totalCount(repository.count(AuditEventSpecifications.of(null)))
        .build();
  }

  /**
   * Builds a page request from the pagination input.
   *
   * @param pagination the pagination input, or null
   * @return a safe page request
   */
  private Pageable toPageable(PaginationInput pagination) {
    Integer page = pagination == null ? null : pagination.getPage();
    Integer size = pagination == null ? null : pagination.getSize();
    String sortBy = pagination == null ? null : pagination.getSortBy();
    String sortDir = pagination == null || pagination.getSortDir() == null
        ? null
        : pagination.getSortDir().name();
    return PageRequests.of(page, size, sortBy, sortDir, "createdAt", Sort.Direction.DESC);
  }

  /**
   * Parses a UUID, tolerating missing or malformed values.
   *
   * @param value the string to parse
   * @return the UUID, or null when blank or invalid
   */
  private static UUID parseUuid(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}

package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.base.util.FilterBuilder;
import com.sun.base.util.FilterSpec;
import com.sun.gaia.service.PermissionService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.repository.ReaderTextRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Service for reader texts.
 */
@Service
public class ReaderTextService extends BaseService<ReaderTextEntity> {

  private final ReaderTextRepository textRepository;

  public ReaderTextService(ReaderTextRepository repository) {
    super(repository);
    this.textRepository = repository;
  }

  /**
   * Lists active texts, filtered by the given generic filter specs.
   *
   * @param filters optional filter specs from PaginationInput
   * @param pageable the page request
   * @return a page of matching texts
   */
  public Page<ReaderTextEntity> list(List<FilterSpec> filters, Pageable pageable) {
    return textRepository.findAll(active(filters), pageable);
  }

  /**
   * Lists active text summaries, using a projection that skips the text body.
   *
   * @param filters optional filter specs from PaginationInput
   * @param pageable the page request
   * @return a page of summaries
   */
  public Page<ReaderTextSummary> listSummaries(List<FilterSpec> filters, Pageable pageable) {
    if (filters == null || filters.isEmpty()) {
      return textRepository.findSummaries(ReaderTextStatus.ACTIVE, pageable);
    }
    return textRepository.findAll(active(filters), pageable).map(this::toSummary);
  }

  /**
   * Lists active text summaries without running a count query.
   *
   * @param pageable the page request
   * @return a slice of summaries
   */
  public Slice<ReaderTextSummary> listSummaries(Pageable pageable) {
    return textRepository.findSummariesSlice(ReaderTextStatus.ACTIVE, pageable);
  }

  /**
   * Reads the exact active-text count from the maintained counter.
   *
   * @return the active text count
   */
  public long countActive() {
    return textRepository.findCount(ReaderTextStatus.ACTIVE.name()).orElse(0L);
  }

  /**
   * Builds the active-status specification with any caller filters applied.
   *
   * @param filters optional filter specs
   * @return the specification
   */
  private Specification<ReaderTextEntity> active(List<FilterSpec> filters) {
    Specification<ReaderTextEntity> spec = (root, q, b) ->
        b.equal(root.get("status"), ReaderTextStatus.ACTIVE);
    Specification<ReaderTextEntity> filterSpec = FilterBuilder.buildFilters(filters);
    return filterSpec == null ? spec : spec.and(filterSpec);
  }

  /**
   * Maps an entity to the list projection.
   *
   * @param entity the text entity
   * @return the summary
   */
  private ReaderTextSummary toSummary(ReaderTextEntity entity) {
    return new ReaderTextSummary(entity.getId(), entity.getTitle(), entity.getLanguage(),
        entity.getLevel(), entity.getOwnerId(), entity.getSourceId(), entity.getStatus(),
        entity.getCreatedAt(), entity.getLastUpdatedAt());
  }
}

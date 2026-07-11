package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.base.util.FilterBuilder;
import com.sun.base.util.FilterSpec;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.repository.ReaderTextRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Specification<ReaderTextEntity> spec = (root, q, b) ->
        b.equal(root.get("status"), ReaderTextStatus.ACTIVE);
    Specification<ReaderTextEntity> filterSpec = FilterBuilder.buildFilters(filters);
    if (filterSpec != null) {
      spec = spec.and(filterSpec);
    }
    return textRepository.findAll(spec, pageable);
  }
}

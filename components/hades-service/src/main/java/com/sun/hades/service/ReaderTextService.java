package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.model.enums.ReaderTextType;
import com.sun.hades.repository.ReaderTextRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for reader texts.
 */
@Service
@Transactional
public class ReaderTextService extends BaseService<ReaderTextEntity> {

  private final ReaderTextRepository textRepository;

  public ReaderTextService(ReaderTextRepository repository) {
    super(repository);
    this.textRepository = repository;
  }

  /**
   * Lists active texts, optionally filtered by level, source, and type.
   *
   * @param level optional CEFR level filter
   * @param sourceId optional source id filter
   * @param type optional type filter
   * @param pageable the page request
   * @return a page of matching texts
   */
  public Page<ReaderTextEntity> list(
      CefrLevel level, UUID sourceId, ReaderTextType type, Pageable pageable) {
    Specification<ReaderTextEntity> spec = (root, q, b) ->
        b.equal(root.get("status"), ReaderTextStatus.ACTIVE);
    if (level != null) {
      spec = spec.and((root, q, b) -> b.equal(root.get("level"), level));
    }
    if (sourceId != null) {
      spec = spec.and((root, q, b) -> b.equal(root.get("sourceId"), sourceId));
    }
    if (type != null) {
      spec = spec.and((root, q, b) -> b.equal(root.get("type"), type));
    }
    return textRepository.findAll(spec, pageable);
  }
}

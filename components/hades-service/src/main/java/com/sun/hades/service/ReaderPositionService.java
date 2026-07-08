package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.hades.model.ReaderPositionEntity;
import com.sun.hades.repository.ReaderPositionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for character-range positions on a text.
 */
@Service
@Transactional("hadesTransactionManager")
public class ReaderPositionService extends BaseService<ReaderPositionEntity> {

  private final ReaderPositionRepository positionRepository;

  public ReaderPositionService(ReaderPositionRepository repository) {
    super(repository);
    this.positionRepository = repository;
  }

  /**
   * Lists every position on a text.
   *
   * @param textId the text id
   * @return the positions
   */
  public List<ReaderPositionEntity> listForText(UUID textId) {
    return positionRepository.findByTextId(textId);
  }

  /**
   * Finds a position for an exact range, if present.
   *
   * @param textId the text id
   * @param startOffset the start offset
   * @param endOffset the end offset
   * @return the position, or empty
   */
  public Optional<ReaderPositionEntity> findExact(
      UUID textId, int startOffset, int endOffset) {
    return positionRepository.findByTextIdAndStartOffsetAndEndOffset(
        textId, startOffset, endOffset);
  }
}

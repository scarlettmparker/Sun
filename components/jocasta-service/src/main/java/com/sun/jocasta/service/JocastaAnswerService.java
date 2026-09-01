package com.sun.jocasta.service;

import com.sun.jocasta.model.AnswerEntity;
import com.sun.jocasta.repository.AnswerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class JocastaAnswerService {

  private final AnswerRepository repository;

  public JocastaAnswerService(AnswerRepository repository) {
    this.repository = repository;
  }

  /**
   * Submits answer.
   *
   * @param entity - answer entity
   * @return saved
   */
  public AnswerEntity submit(AnswerEntity entity) {
    return repository.save(entity);
  }

  /**
   * Lists answers for question.
   *
   * @param questionId - question id
   * @param pageable - pagination
   * @return page
   */
  public Page<AnswerEntity> listByQuestion(UUID questionId, Pageable pageable) {
    return repository.findByQuestionId(questionId, pageable);
  }

  /**
   * Finds answer by id.
   *
   * @param id - answer id
   * @return entity
   */
  public Optional<AnswerEntity> findById(UUID id) {
    return repository.findById(id);
  }
}

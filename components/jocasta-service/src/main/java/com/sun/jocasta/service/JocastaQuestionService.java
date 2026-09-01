package com.sun.jocasta.service;

import com.sun.jocasta.model.QuestionEntity;
import com.sun.jocasta.repository.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class JocastaQuestionService {

  private final QuestionRepository repository;

  public JocastaQuestionService(QuestionRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates questions in bulk.
   *
   * @param entities - question entities
   * @return saved entities
   */
  public List<QuestionEntity> createBulk(List<QuestionEntity> entities) {
    return repository.saveAll(entities);
  }

  /**
   * Lists questions for remote object.
   *
   * @param target - remote object string
   * @param pageable - pagination
   * @return page
   */
  public Page<QuestionEntity> listByRemoteObject(String target, Pageable pageable) {
    return repository.findByRemoteObject(target, pageable);
  }

  /**
   * Finds question by id.
   *
   * @param id - question id
   * @return entity
   */
  public Optional<QuestionEntity> findById(UUID id) {
    return repository.findById(id);
  }

  /**
   * Links question to additional remote object.
   *
   * @param id - question id
   * @param target - remote object target
   * @return updated entity
   */
  public Optional<QuestionEntity> linkQuestion(UUID id, String target) {
    Optional<QuestionEntity> opt = repository.findById(id);
    if (opt.isEmpty()) return Optional.empty();
    QuestionEntity entity = opt.get();
    List<String> remote = entity.getRemoteObject();
    if (remote == null) remote = new ArrayList<>();
    else remote = new ArrayList<>(remote);
    if (!remote.contains(target)) {
      remote.add(target);
      entity.setRemoteObject(remote);
      return Optional.of(repository.save(entity));
    }
    return Optional.of(entity);
  }
}

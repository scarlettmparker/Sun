package com.sun.hades.service;

import com.sun.hades.model.TextVersionEntity;
import com.sun.hades.repository.TextVersionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for text version history.
 */
@Service
public class TextVersionService {

  private final TextVersionRepository repository;

  public TextVersionService(TextVersionRepository repository) {
    this.repository = repository;
  }

  /**
   * Lists versions for a text, newest first.
   *
   * @param textId the text id
   * @return the versions
   */
  @Transactional(readOnly = true)
  public List<TextVersionEntity> listForText(UUID textId) {
    return repository.findByTextIdOrderByVersionDesc(textId);
  }
}

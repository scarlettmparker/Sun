package com.sun.medusa.service;

import com.sun.medusa.model.BoardLayoutEntity;
import com.sun.medusa.repository.BoardLayoutRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BoardLayoutService {

  private final BoardLayoutRepository repository;

  public BoardLayoutService(BoardLayoutRepository repository) {
    this.repository = repository;
  }

  /**
   * Lists all board layouts.
   *
   * @return the layouts
   */
  public List<BoardLayoutEntity> list() {
    return repository.findAll();
  }

  /**
   * Finds a board layout by id.
   *
   * @param id the layout id
   * @return the layout, if found
   */
  public Optional<BoardLayoutEntity> findById(UUID id) {
    return repository.findById(id);
  }

  /**
   * Finds a board layout by slug.
   *
   * @param slug the slug
   * @return the layout, if found
   */
  public Optional<BoardLayoutEntity> findBySlug(String slug) {
    return repository.findBySlug(slug);
  }

  /**
   * Saves a board layout.
   *
   * @param layout the layout
   * @return the saved layout
   */
  public BoardLayoutEntity save(BoardLayoutEntity layout) {
    if (layout.getId() != null) {
      BoardLayoutEntity existing = repository.findById(layout.getId()).orElse(null);
      if (existing != null && existing.isSystem()) {
        throw new IllegalStateException("System layouts cannot be edited");
      }
    }
    return repository.save(layout);
  }

  /**
   * Deletes a non-system board layout.
   *
   * @param id the layout id
   * @return true when deleted
   */
  public boolean delete(UUID id) {
    BoardLayoutEntity existing = repository.findById(id).orElse(null);
    if (existing == null || existing.isSystem()) {
      return false;
    }
    repository.deleteById(id);
    return true;
  }
}
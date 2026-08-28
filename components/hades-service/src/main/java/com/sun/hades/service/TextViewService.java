package com.sun.hades.service;

import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.TextViewEntity;
import com.sun.hades.repository.TextViewRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks which texts a user has viewed.
 */
@Service
public class TextViewService {

  private final TextViewRepository repository;

  public TextViewService(TextViewRepository repository) {
    this.repository = repository;
  }

  /**
   * Marks a text as viewed by the current user.
   *
   * @param textId the text id
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markViewed(UUID textId) {
    UUID viewer = requireUser();
    repository.upsert(viewer, textId);
  }

  /**
   * Lists viewed texts for the current user.
   *
   * @param pageable the page request
   * @return a page of views
   */
  @Transactional(readOnly = true)
  public Page<TextViewEntity> viewedTexts(Pageable pageable) {
    UUID viewer = requireUser();
    return repository.findByAccountId(viewer, pageable);
  }

  /**
   * Returns the set of viewed text ids among the given candidates.
   *
   * @param textIds the candidate ids
   * @return the viewed subset
   */
  @Transactional(readOnly = true)
  public Set<UUID> viewedIds(List<UUID> textIds) {
    UUID viewer = requireUser();
    if (textIds == null || textIds.isEmpty()) {
      return Set.of();
    }
    return repository.findByAccountIdAndTextIdIn(viewer, textIds).stream()
        .map(TextViewEntity::getTextId)
        .collect(Collectors.toSet());
  }

  private UUID requireUser() {
    UUID id = UserContextHolder.getUserId();
    if (id == null) {
      throw new IllegalArgumentException("Authentication required");
    }
    return id;
  }
}

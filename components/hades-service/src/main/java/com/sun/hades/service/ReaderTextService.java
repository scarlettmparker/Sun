package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.base.util.FilterBuilder;
import com.sun.base.util.FilterSpec;
import com.sun.gaia.service.PermissionService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.TextVersionEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.repository.ReaderTextRepository;
import com.sun.hades.repository.TextVersionRepository;
import java.util.List;
import java.util.Locale;
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
public class ReaderTextService extends BaseService<ReaderTextEntity> {

  private final ReaderTextRepository textRepository;
  private final TextVersionRepository versionRepository;
  private final PermissionService permissionService;

  public ReaderTextService(
      ReaderTextRepository repository,
      TextVersionRepository versionRepository,
      PermissionService permissionService) {
    super(repository);
    this.textRepository = repository;
    this.versionRepository = versionRepository;
    this.permissionService = permissionService;
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

  /**
   * Edits a text, snapshotting the previous version.
   *
   * @param id the text id
   * @param title the new title
   * @param content the new content
   * @param language the language tag
   * @param level the CEFR level
   * @param sourceId the source id or null
   * @return the text id
   */
  @Transactional
  public UUID editText(
      UUID id, String title, String content, String language, CefrLevel level, String sourceId) {
    ReaderTextEntity text =
        textRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Text not found: " + id));
    UUID viewer = requireUser();
    UUID ownerId = text.getOwnerId();
    if (ownerId != null
        && !ownerId.equals(viewer)
        && !permissionService.has("graphql.hades.editText")) {
      throw new IllegalArgumentException("Not authorized to edit text");
    }
    validateLanguage(language);
    int nextVersion = versionRepository.findMaxVersionByTextId(id) + 1;
    TextVersionEntity snapshot = new TextVersionEntity();
    snapshot.setTextId(id);
    snapshot.setVersion(nextVersion);
    snapshot.setTitle(text.getTitle());
    snapshot.setContent(text.getContent());
    snapshot.setLevel(text.getLevel());
    snapshot.setLanguage(text.getLanguage());
    snapshot.setEditedBy(viewer);
    versionRepository.save(snapshot);

    text.setTitle(title);
    text.setContent(content);
    if (level != null) {
      text.setLevel(level);
    }
    text.setLanguage(language);
    if (sourceId != null) {
      text.setSourceId(UUID.fromString(sourceId));
    }
    return textRepository.save(text).getId();
  }

  private void validateLanguage(String language) {
    if (language == null || language.isBlank()) {
      throw new IllegalArgumentException("Invalid language tag");
    }
    String tag = language.trim();
    Locale locale = Locale.forLanguageTag(tag);
    if (locale.getLanguage() == null || locale.getLanguage().isBlank()) {
      throw new IllegalArgumentException("Invalid language tag");
    }
  }

  private UUID requireUser() {
    UUID id = UserContextHolder.getUserId();
    if (id == null) {
      throw new IllegalArgumentException("Authentication required");
    }
    return id;
  }
}

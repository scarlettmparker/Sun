package com.sun.echo.service;

import com.sun.base.service.BaseService;
import com.sun.echo.model.ChecklistEntryEntity;
import com.sun.echo.model.ChecklistEntryItemEntity;
import com.sun.echo.model.ChecklistTemplateItemEntity;
import com.sun.echo.model.enums.ChecklistStatus;
import com.sun.echo.model.enums.ItemStatus;
import com.sun.echo.repository.ChecklistEntryItemRepository;
import com.sun.echo.repository.ChecklistEntryRepository;
import com.sun.echo.repository.ChecklistTemplateItemRepository;
import com.sun.echo.repository.ChecklistTemplateRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing checklist entry entities. */
@Service
@Transactional("echoTransactionManager")
public class ChecklistEntryService extends BaseService<ChecklistEntryEntity> {

  private final ChecklistTemplateRepository templateRepository;
  private final ChecklistTemplateItemRepository templateItemRepository;
  private final ChecklistEntryItemRepository entryItemRepository;

  public ChecklistEntryService(ChecklistEntryRepository repository,
      ChecklistTemplateRepository templateRepository,
      ChecklistTemplateItemRepository templateItemRepository,
      ChecklistEntryItemRepository entryItemRepository) {
    super(repository);
    this.templateRepository = templateRepository;
    this.templateItemRepository = templateItemRepository;
    this.entryItemRepository = entryItemRepository;
  }

  /**
   * Locates a checklist entry by id.
   *
   * @param id the entry id
   * @return the entry, or empty if not found
   */
  public Optional<ChecklistEntryEntity> locate(UUID id) {
    return findById(id);
  }

  /**
   * Marks a checklist entry as complete by stamping its completion timestamp.
   * A completed entry cannot go back to incomplete, so re-calling is a no-op.
   *
   * @param id the entry id
   * @return the updated entry
   */
  public ChecklistEntryEntity completeChecklist(UUID id) {
    ChecklistEntryEntity entry = findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Checklist entry not found: " + id));
    if (entry.getCompletedAt() != null) {
      return entry;
    }
    entry.setCompletedAt(LocalDateTime.now());
    return save(entry);
  }

  /**
   * Archives a checklist entry. Entry items are preserved so history is kept.
   *
   * @param id the entry id
   * @return the updated entry
   */
  public ChecklistEntryEntity archive(UUID id) {
    ChecklistEntryEntity entry = findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Checklist entry not found: " + id));
    entry.setStatus(ChecklistStatus.ARCHIVED);
    return save(entry);
  }

  /**
   * Permanently deletes a checklist entry and all of its items.
   *
   * @param id the entry id
   */
  public void delete(UUID id) {
    findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Checklist entry not found: " + id));
    entryItemRepository.deleteByEntryId(id);
    deleteById(id);
  }

  /**
   * Creates a new checklist entry from a template, cloning each template item
   * as an entry item with status NOT_STARTED. The entry is named after the
   * template unless an explicit name is supplied.
   *
   * @param templateId the template id
   * @param name an optional name overriding the template's name
   * @return the new entry
   */
  public ChecklistEntryEntity createFromTemplate(UUID templateId, String name) {
    ChecklistEntryEntity entry = new ChecklistEntryEntity();
    if (name != null && !name.isBlank()) {
      entry.setName(name);
    } else {
      templateRepository.findById(templateId).ifPresent(t -> entry.setName(t.getName()));
    }
    ChecklistEntryEntity saved = save(entry);

    List<ChecklistTemplateItemEntity> templateItems =
        templateItemRepository.findByTemplateIdOrderByPositionAsc(templateId);
    for (ChecklistTemplateItemEntity ti : templateItems) {
      ChecklistEntryItemEntity ei = new ChecklistEntryItemEntity();
      ei.setEntryId(saved.getId());
      ei.setItemId(ti.getItemId());
      ei.setPosition(ti.getPosition());
      ei.setStatus(ItemStatus.NOT_STARTED);
      entryItemRepository.save(ei);
    }
    return saved;
  }

  /**
   * Creates a new checklist entry composed from multiple templates, merging
   * their items (de-duplicated by item id) in template order.
   *
   * @param templateIds the template ids to compose
   * @param name an optional name for the new entry
   * @return the new entry
   */
  public ChecklistEntryEntity createFromTemplates(List<UUID> templateIds, String name) {
    ChecklistEntryEntity entry = new ChecklistEntryEntity();
    entry.setName(name);
    ChecklistEntryEntity saved = save(entry);

    Set<UUID> seen = new HashSet<>();
    int position = 0;
    for (UUID templateId : templateIds) {
      for (ChecklistTemplateItemEntity ti
          : templateItemRepository.findByTemplateIdOrderByPositionAsc(templateId)) {
        if (!seen.add(ti.getItemId())) {
          continue;
        }
        ChecklistEntryItemEntity ei = new ChecklistEntryItemEntity();
        ei.setEntryId(saved.getId());
        ei.setItemId(ti.getItemId());
        ei.setPosition(position++);
        ei.setStatus(ItemStatus.NOT_STARTED);
        entryItemRepository.save(ei);
      }
    }
    return saved;
  }
}

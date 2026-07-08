package com.sun.echo.service;

import com.sun.base.service.BaseService;
import com.sun.echo.model.ChecklistEntryItemEntity;
import com.sun.echo.model.enums.ItemStatus;
import com.sun.echo.repository.ChecklistEntryItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing the items linked to checklist entries. */
@Service
@Transactional
public class ChecklistEntryItemService extends BaseService<ChecklistEntryItemEntity> {

  private final ChecklistEntryItemRepository entryItemRepository;

  public ChecklistEntryItemService(ChecklistEntryItemRepository repository) {
    super(repository);
    this.entryItemRepository = repository;
  }

  /**
   * Lists the items in an entry, ordered by position.
   *
   * @param entryId the entry id
   * @return the entry items
   */
  public List<ChecklistEntryItemEntity> listForEntry(UUID entryId) {
    return entryItemRepository.findByEntryIdOrderByPositionAsc(entryId);
  }

  /**
   * Lists the items in an entry as a page.
   *
   * @param entryId the entry id
   * @param pageable the page request
   * @return the page of entry items
   */
  public Page<ChecklistEntryItemEntity> listForEntryPaged(UUID entryId, Pageable pageable) {
    return entryItemRepository.findByEntryId(entryId, pageable);
  }

  /**
   * Adds an item to an entry. When position is null it is set to one more than
   * the current highest position.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param position an optional explicit position
   * @return the new entry item
   */
  public ChecklistEntryItemEntity addItem(UUID entryId, UUID itemId, Integer position) {
    Optional<ChecklistEntryItemEntity> existing =
        entryItemRepository.findByEntryIdAndItemId(entryId, itemId);
    if (existing.isPresent()) {
      return existing.get();
    }

    ChecklistEntryItemEntity entity = new ChecklistEntryItemEntity();
    entity.setEntryId(entryId);
    entity.setItemId(itemId);
    int pos = position != null ? position : entryItemRepository.findMaxPositionByEntryId(entryId) + 1;
    entity.setPosition(pos);
    entity.setStatus(ItemStatus.NOT_STARTED);
    return entryItemRepository.save(entity);
  }

  /**
   * Removes an item from an entry.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @return true if an item was removed
   */
  public boolean removeItem(UUID entryId, UUID itemId) {
    return entryItemRepository.deleteByEntryIdAndItemId(entryId, itemId) > 0;
  }

  /**
   * Sets the status of an item within an entry.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param status the new status
   * @return the updated entry item
   */
  public ChecklistEntryItemEntity setStatus(UUID entryId, UUID itemId, ItemStatus status) {
    ChecklistEntryItemEntity entity = entryItemRepository.findByEntryIdAndItemId(entryId, itemId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Entry item not found for entry " + entryId + " / item " + itemId));
    entity.setStatus(status);
    return entryItemRepository.save(entity);
  }
}

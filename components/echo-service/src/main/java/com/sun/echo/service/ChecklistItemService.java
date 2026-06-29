package com.sun.echo.service;

import com.sun.base.service.BaseService;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.model.enums.LifecycleStatus;
import com.sun.echo.repository.ChecklistItemRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing checklist item entities. */
@Service
@Transactional("echoTransactionManager")
public class ChecklistItemService extends BaseService<ChecklistItemEntity> {

  private final ChecklistItemRepository checklistItemRepository;

  public ChecklistItemService(ChecklistItemRepository repository) {
    super(repository);
    this.checklistItemRepository = repository;
  }

  /**
   * Locates a checklist item by id.
   *
   * @param id the item id
   * @return the item, or empty if not found
   */
  public Optional<ChecklistItemEntity> locate(UUID id) {
    return findById(id);
  }

  /**
   * Marks a checklist item as retired. Existing entries keep their references;
   * the item simply stops being selectable for new checklists.
   *
   * @param id the item id
   * @return the updated item
   */
  public ChecklistItemEntity retire(UUID id) {
    ChecklistItemEntity item = findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Checklist item not found: " + id));
    item.setLifecycleStatus(LifecycleStatus.RETIRED);
    return save(item);
  }
}

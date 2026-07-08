package com.sun.echo.service;

import com.sun.base.service.BaseService;
import com.sun.echo.model.ChecklistTemplateEntity;
import com.sun.echo.model.ChecklistTemplateItemEntity;
import com.sun.echo.model.enums.ChecklistStatus;
import com.sun.echo.repository.ChecklistTemplateItemRepository;
import com.sun.echo.repository.ChecklistTemplateRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing checklist template entities. */
@Service
@Transactional
public class ChecklistTemplateService extends BaseService<ChecklistTemplateEntity> {

  private final ChecklistTemplateItemRepository templateItemRepository;

  public ChecklistTemplateService(ChecklistTemplateRepository repository,
      ChecklistTemplateItemRepository templateItemRepository) {
    super(repository);
    this.templateItemRepository = templateItemRepository;
  }

  /**
   * Locates a checklist template by id.
   *
   * @param id the template id
   * @return the template, or empty if not found
   */
  public Optional<ChecklistTemplateEntity> locate(UUID id) {
    return findById(id);
  }

  /**
   * Archives a checklist template.
   *
   * @param id the template id
   * @return the updated template
   */
  public ChecklistTemplateEntity archive(UUID id) {
    ChecklistTemplateEntity template = findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Checklist template not found: " + id));
    template.setStatus(ChecklistStatus.ARCHIVED);
    return save(template);
  }

  /**
   * Adds a set of items to a template, appending each at the next position.
   *
   * @param templateId the template id
   * @param itemIds the item ids to add
   */
  public void addItems(UUID templateId, List<UUID> itemIds) {
    if (itemIds == null || itemIds.isEmpty()) {
      return;
    }
    int position = templateItemRepository.findMaxPositionByTemplateId(templateId) + 1;
    for (UUID itemId : itemIds) {
      ChecklistTemplateItemEntity ti = new ChecklistTemplateItemEntity();
      ti.setTemplateId(templateId);
      ti.setItemId(itemId);
      ti.setPosition(position++);
      templateItemRepository.save(ti);
    }
  }
}

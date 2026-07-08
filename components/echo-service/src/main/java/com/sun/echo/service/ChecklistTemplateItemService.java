package com.sun.echo.service;

import com.sun.base.service.BaseService;
import com.sun.echo.model.ChecklistTemplateItemEntity;
import com.sun.echo.repository.ChecklistTemplateItemRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing the items linked to checklist templates. */
@Service
@Transactional
public class ChecklistTemplateItemService extends BaseService<ChecklistTemplateItemEntity> {

  private final ChecklistTemplateItemRepository templateItemRepository;

  public ChecklistTemplateItemService(ChecklistTemplateItemRepository repository) {
    super(repository);
    this.templateItemRepository = repository;
  }

  /**
   * Lists the items in a template, ordered by position.
   *
   * @param templateId the template id
   * @return the template items
   */
  public List<ChecklistTemplateItemEntity> listForTemplate(UUID templateId) {
    return templateItemRepository.findByTemplateIdOrderByPositionAsc(templateId);
  }

  /**
   * Lists the items in a template as a page.
   *
   * @param templateId the template id
   * @param pageable the page request
   * @return the page of template items
   */
  public Page<ChecklistTemplateItemEntity> listForTemplatePaged(UUID templateId, Pageable pageable) {
    return templateItemRepository.findByTemplateId(templateId, pageable);
  }

  /**
   * Adds an item to a template. When position is null it is set to one more than
   * the current highest position.
   *
   * @param templateId the template id
   * @param itemId the item id
   * @param position an optional explicit position
   * @return the new template item
   */
  public ChecklistTemplateItemEntity addTemplateItem(UUID templateId, UUID itemId, Integer position) {
    ChecklistTemplateItemEntity entity = new ChecklistTemplateItemEntity();
    entity.setTemplateId(templateId);
    entity.setItemId(itemId);
    int pos = position != null ? position : templateItemRepository.findMaxPositionByTemplateId(templateId) + 1;
    entity.setPosition(pos);
    return templateItemRepository.save(entity);
  }

  /**
   * Removes an item from a template.
   *
   * @param templateId the template id
   * @param itemId the item id
   * @return true if an item was removed
   */
  public boolean removeTemplateItem(UUID templateId, UUID itemId) {
    return templateItemRepository.deleteByTemplateIdAndItemId(templateId, itemId) > 0;
  }
}

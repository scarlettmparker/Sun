package com.sun.echo.service;

import com.sun.base.service.BaseService;
import com.sun.echo.model.ChecklistCategoryEntity;
import com.sun.echo.repository.ChecklistCategoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing checklist category entities. */
@Service
@Transactional
public class ChecklistCategoryService extends BaseService<ChecklistCategoryEntity> {

  public ChecklistCategoryService(ChecklistCategoryRepository repository) {
    super(repository);
  }

  /**
   * Locates a checklist category by id.
   *
   * @param id the category id
   * @return the category, or empty if not found
   */
  public Optional<ChecklistCategoryEntity> locate(UUID id) {
    return findById(id);
  }
}

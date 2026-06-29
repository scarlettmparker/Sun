package com.sun.echo.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.echo.model.ChecklistItemEntity;
import java.util.UUID;

public interface ChecklistItemRepository extends BaseRepository<ChecklistItemEntity> {

  long countByCategoryId(UUID categoryId);
}

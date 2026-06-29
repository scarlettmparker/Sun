package com.sun.echo.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.echo.model.ChecklistTemplateItemEntity;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistTemplateItemRepository extends BaseRepository<ChecklistTemplateItemEntity> {

  List<ChecklistTemplateItemEntity> findByTemplateIdOrderByPositionAsc(UUID templateId);

  Optional<ChecklistTemplateItemEntity> findByTemplateIdAndItemId(UUID templateId, UUID itemId);

  int deleteByTemplateIdAndItemId(UUID templateId, UUID itemId);

  @Query(value = "SELECT COALESCE(MAX(position), -1) FROM checklist_template_items WHERE template_id = ?1",
      nativeQuery = true)
  Integer findMaxPositionByTemplateId(UUID templateId);
}

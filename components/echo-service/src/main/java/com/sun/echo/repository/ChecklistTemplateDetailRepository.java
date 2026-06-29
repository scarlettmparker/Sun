package com.sun.echo.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.echo.model.ChecklistTemplateDetailEntity;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistTemplateDetailRepository extends BaseRepository<ChecklistTemplateDetailEntity> {

  Optional<ChecklistTemplateDetailEntity> findByOwnerId(UUID ownerId);

  @Query(value = "SELECT * FROM checklist_template_details WHERE EXISTS "
      + "(SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = ANY(?1))",
      nativeQuery = true)
  List<ChecklistTemplateDetailEntity> findByRemoteObjectsIn(String[] ids);
}

package com.sun.echo.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.echo.model.ChecklistItemDetailEntity;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistItemDetailRepository extends BaseRepository<ChecklistItemDetailEntity> {

  Optional<ChecklistItemDetailEntity> findByOwnerId(UUID ownerId);

  @Query(value = "SELECT * FROM echo_checklist_item_details WHERE EXISTS "
      + "(SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = ANY(?1))",
      nativeQuery = true)
  List<ChecklistItemDetailEntity> findByRemoteObjectsIn(String[] ids);
}

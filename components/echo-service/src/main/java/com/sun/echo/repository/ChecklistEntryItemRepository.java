package com.sun.echo.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.echo.model.ChecklistEntryItemEntity;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistEntryItemRepository extends BaseRepository<ChecklistEntryItemEntity> {

  List<ChecklistEntryItemEntity> findByEntryIdOrderByPositionAsc(UUID entryId);

  Optional<ChecklistEntryItemEntity> findByEntryIdAndItemId(UUID entryId, UUID itemId);

  int deleteByEntryIdAndItemId(UUID entryId, UUID itemId);

  @Query(value = "SELECT COALESCE(MAX(position), -1) FROM checklist_entry_items WHERE entry_id = ?1",
      nativeQuery = true)
  Integer findMaxPositionByEntryId(UUID entryId);
}

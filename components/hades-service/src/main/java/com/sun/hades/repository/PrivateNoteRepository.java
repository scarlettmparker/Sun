package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.PrivateNoteEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrivateNoteRepository
    extends BaseRepository<PrivateNoteEntity>, JpaSpecificationExecutor<PrivateNoteEntity> {

  List<PrivateNoteEntity> findByTextId(UUID textId);

  List<PrivateNoteEntity> findByOwnerId(UUID ownerId);

  @Query(value = "SELECT * FROM hades_private_notes WHERE EXISTS "
      + "(SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = ANY(?1))",
      nativeQuery = true)
  List<PrivateNoteEntity> findByRemoteObjectsIn(String[] ids);

  @Query("select n from PrivateNoteEntity n where n.ownerId = :ownerId and n.textId = :textId")
  List<PrivateNoteEntity> findByOwnerIdAndTextId(
      @Param("ownerId") UUID ownerId, @Param("textId") UUID textId);
}

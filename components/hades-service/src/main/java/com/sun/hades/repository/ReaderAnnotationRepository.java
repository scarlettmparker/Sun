package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderAnnotationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReaderAnnotationRepository extends BaseRepository<ReaderAnnotationEntity> {

  @Query("select a from ReaderAnnotationEntity a where a.positionId in "
      + "(select p.id from ReaderPositionEntity p where p.textId = :textId)")
  List<ReaderAnnotationEntity> findByTextId(@Param("textId") UUID textId);

  long countByPositionId(UUID positionId);

  @Query(value = "SELECT * FROM reader_annotations WHERE EXISTS "
      + "(SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = ANY(?1))",
      nativeQuery = true)
  List<ReaderAnnotationEntity> findByRemoteObjectsIn(String[] ids);
}

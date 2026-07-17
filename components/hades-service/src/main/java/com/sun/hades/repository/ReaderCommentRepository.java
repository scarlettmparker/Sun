package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderCommentEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReaderCommentRepository extends BaseRepository<ReaderCommentEntity> {

  Page<ReaderCommentEntity> findByAnnotationId(UUID annotationId, Pageable pageable);

  /**
   * Counts active comments per annotation id for a batch of annotations.
   *
   * @param annotationIds the annotations to count replies for
   * @return rows of [annotationId, count]
   */
  @Query(
      "select c.annotationId, count(c) from ReaderCommentEntity c "
          + "where c.annotationId in :annotationIds "
          + "and c.status = com.sun.hades.model.enums.ReaderStatus.ACTIVE "
          + "group by c.annotationId")
  List<Object[]> countActiveByAnnotationIdIn(
      @Param("annotationIds") Collection<UUID> annotationIds);
}

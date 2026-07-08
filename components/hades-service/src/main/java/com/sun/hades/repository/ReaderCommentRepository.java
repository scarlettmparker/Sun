package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderCommentEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReaderCommentRepository extends BaseRepository<ReaderCommentEntity> {

  Page<ReaderCommentEntity> findByAnnotationId(UUID annotationId, Pageable pageable);
}

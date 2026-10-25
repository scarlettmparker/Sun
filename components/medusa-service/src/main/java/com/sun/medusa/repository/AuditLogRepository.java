package com.sun.medusa.repository;

import com.sun.medusa.model.AuditLogEntity;
import com.sun.base.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository extends BaseRepository<AuditLogEntity> {
  Page<AuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
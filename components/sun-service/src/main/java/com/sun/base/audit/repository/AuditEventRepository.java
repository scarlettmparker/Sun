package com.sun.base.audit.repository;

import com.sun.base.audit.entity.AuditEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for AuditEvent rows. Insert and read only - the table is append-only
 * both here (no update/delete methods) and at the database (triggers).
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

  /**
   * Most recent audit row, used to seed the global hash chain.
   *
   * @return the latest row, or empty when none exist yet
   */
  Optional<AuditEvent> findFirstByOrderByCreatedAtDescIdDesc();

  /**
   * Every row written for one request.
   *
   * @param correlationId the request correlation id
   * @return the rows for that request
   */
  List<AuditEvent> findByCorrelationId(UUID correlationId);
}

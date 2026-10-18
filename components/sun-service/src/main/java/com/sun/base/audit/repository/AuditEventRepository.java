package com.sun.base.audit.repository;

import com.sun.base.audit.entity.AuditEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository for AuditEvent rows. Insert and read only, enforced here by the
 * absence of update and delete methods and in the database by triggers.
 */
public interface AuditEventRepository
    extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {

  /**
   * Finds the most recent row in a namespace, to chain the next row's hash.
   *
   * @param namespace the namespace to scope to
   * @return the latest row, or empty when the namespace has none
   */
  Optional<AuditEvent> findFirstByNamespaceOrderByCreatedAtDescIdDesc(String namespace);

  /**
   * Finds the most recent row with no namespace, to keep that chain intact.
   *
   * @return the latest row with no namespace, or empty
   */
  Optional<AuditEvent> findFirstByNamespaceIsNullOrderByCreatedAtDescIdDesc();

  /**
   * Finds every row written for one request.
   *
   * @param correlationId the request correlation id
   * @return the rows for that request
   */
  List<AuditEvent> findByCorrelationId(UUID correlationId);

  /**
   * Finds every row written for one request within a namespace.
   *
   * @param correlationId the request correlation id
   * @param namespace the namespace to scope to
   * @return the rows for that request
   */
  List<AuditEvent> findByCorrelationIdAndNamespace(UUID correlationId, String namespace);

  /**
   * Finds a single row scoped to a namespace.
   *
   * @param id the audit event id
   * @param namespace the namespace to scope to
   * @return the row, or empty when absent or in another namespace
   */
  Optional<AuditEvent> findByIdAndNamespace(UUID id, String namespace);

  /**
   * Finds a page of rows for a namespace, newest first.
   *
   * @param namespace the namespace to scope to
   * @param pageable the page request
   * @return a page of audit events
   */
  Page<AuditEvent> findByNamespaceOrderByCreatedAtDesc(String namespace, Pageable pageable);

  /**
   * Finds a page of rows in chain order, oldest first.
   *
   * @param namespace the namespace to scope to
   * @param pageable the page request, sorted ascending by createdAt and id
   * @return a page of rows in chain order
   */
  Page<AuditEvent> findByNamespace(String namespace, Pageable pageable);

  /**
   * Finds a page of rows with no namespace in chain order, oldest first.
   *
   * @param pageable the page request, sorted ascending by createdAt and id
   * @return a page of rows in chain order
   */
  Page<AuditEvent> findByNamespaceIsNull(Pageable pageable);

  /**
   * Counts events per event type.
   *
   * @param namespace the namespace to scope to, or null for all
   * @return value and count rows
   */
  @Query("""
      select e.eventType as value, count(e) as count
      from AuditEvent e
      where (:namespace is null or e.namespace = :namespace)
      group by e.eventType
      order by e.eventType asc
      """)
  List<FacetCount> countByEventType(String namespace);

  /**
   * Counts events per outcome, most frequent first.
   *
   * @param namespace the namespace to scope to, or null for all
   * @return value and count rows
   */
  @Query("""
      select e.outcome as value, count(e) as count
      from AuditEvent e
      where (:namespace is null or e.namespace = :namespace)
      group by e.outcome
      order by count(e) desc
      """)
  List<FacetCount> countByOutcome(String namespace);

  /**
   * Counts events per acting user, labelled by name.
   *
   * @param namespace the namespace to scope to, or null for all
   * @return value, label and count rows
   */
  @Query("""
      select e.userId as value, e.userName as label, count(e) as count
      from AuditEvent e
      where (:namespace is null or e.namespace = :namespace) and e.userId is not null
      group by e.userId, e.userName
      order by e.userName asc
      """)
  List<FacetCount> countByUser(String namespace);

  /**
   * One facet value and how many events carry it.
   */
  interface FacetCount {

    /**
     * @return the stored value
     */
    Object getValue();

    /**
     * @return the display label, defaulting to the value when absent
     */
    String getLabel();

    /**
     * @return the number of matching events
     */
    long getCount();
  }
}

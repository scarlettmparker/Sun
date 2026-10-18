package com.sun.graphql.audit.graphql;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.audit.annotation.Audited;
import com.sun.base.audit.annotation.NotAudited;
import com.sun.graphql.audit.chain.AuditChainVerifier;
import com.sun.graphql.audit.service.AuditQueryService;
import com.sun.graphql.codegen.types.AuditChainVerification;
import com.sun.graphql.codegen.types.AuditEventFilterInput;
import com.sun.graphql.codegen.types.AuditPage;
import com.sun.graphql.codegen.types.AuditQueries;
import com.sun.graphql.codegen.types.PaginationInput;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Root entry point and operations for the audit trail.
 */
@DgsComponent
public class AuditResolver {

  private final AuditQueryService queryService;
  private final AuditChainVerifier chainVerifier;

  public AuditResolver(AuditQueryService queryService, AuditChainVerifier chainVerifier) {
    this.queryService = queryService;
    this.chainVerifier = chainVerifier;
  }

  /**
   * Provides the audit queries object.
   *
   * @return the AuditQueries builder
   */
  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Query", field = "auditQueries")
  public AuditQueries getAuditQueries() {
    return AuditQueries.newBuilder().build();
  }

  /**
   * Loads a page of the audit trail.
   *
   * @param pagination the pagination input
   * @param filter the optional filters
   * @return the paged events and facets
   */
  @Audited(event = "AUDITEVENT_LIST", entity = "AuditEvent")
  @DgsData(parentType = "AuditQueries", field = "auditPage")
  @PreAuthorize("@permissions.has('graphql.audit.auditPage')")
  public AuditPage auditPage(PaginationInput pagination, AuditEventFilterInput filter) {
    return queryService.page(pagination, filter);
  }

  /**
   * Loads a single audit event.
   *
   * @param id the audit event id
   * @return the event, or null when absent
   */
  @Audited(event = "AUDITEVENT_VIEW", entity = "AuditEvent")
  @DgsData(parentType = "AuditQueries", field = "auditEvent")
  @PreAuthorize("@permissions.has('graphql.audit.auditEvent')")
  public com.sun.graphql.codegen.types.AuditEvent auditEvent(String id) {
    return queryService.locate(id).orElse(null);
  }

  /**
   * Loads every audit event sharing a correlation id.
   *
   * @param correlationId the request correlation id
   * @return matching events
   */
  @Audited(event = "AUDITEVENT_LISTCORRELATION", entity = "AuditEvent")
  @DgsData(parentType = "AuditQueries", field = "auditEventsByCorrelationId")
  @PreAuthorize("@permissions.has('graphql.audit.auditEventsByCorrelationId')")
  public List<com.sun.graphql.codegen.types.AuditEvent> auditEventsByCorrelationId(String correlationId) {
    return queryService.byCorrelation(correlationId);
  }

  /**
   * Recomputes a namespace's audit hash chain.
   *
   * @param namespace the namespace to verify, or null for the unnamed chain
   * @return the verification result
   */
  @Audited(event = "AUDITCHAIN_VERIFY", entity = "AuditEvent")
  @DgsData(parentType = "AuditQueries", field = "verifyAuditChain")
  @PreAuthorize("@permissions.has('graphql.audit.verifyAuditChain')")
  public AuditChainVerification verifyAuditChain(String namespace) {
    AuditChainVerifier.AuditChainVerification result = chainVerifier.verify(namespace);
    return AuditChainVerification.newBuilder()
        .intact(result.intact())
        .checked(result.checked())
        .brokenId(result.brokenId() == null ? null : result.brokenId().toString())
        .brokenAt(result.brokenAt() == null ? null : result.brokenAt().atOffset(java.time.ZoneOffset.UTC))
        .keyed(result.keyed())
        .build();
  }
}

package com.sun.graphql.audit.query;

import com.sun.base.audit.entity.AuditEvent;
import com.sun.graphql.codegen.types.AuditFacetValue;
import com.sun.graphql.codegen.types.AuditOperationType;
import com.sun.graphql.codegen.types.AuditOutcome;
import com.sun.graphql.codegen.types.PageInfo;
import com.sun.base.audit.repository.AuditEventRepository.FacetCount;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Maps audit rows and page metadata onto the generated GraphQL types.
 */
public final class AuditEventMapper {

  private AuditEventMapper() {
  }

  /**
   * Maps an audit row to its GraphQL view.
   *
   * @param e the row to map
   * @return the GraphQL audit event
   */
  public static com.sun.graphql.codegen.types.AuditEvent toGraphQl(AuditEvent e) {
    return com.sun.graphql.codegen.types.AuditEvent.newBuilder()
        .id(e.getId() == null ? null : e.getId().toString())
        .createdAt(e.getCreatedAt() == null ? null : e.getCreatedAt().atOffset(ZoneOffset.UTC))
        .correlationId(e.getCorrelationId() == null ? null : e.getCorrelationId().toString())
        .userId(e.getUserId() == null ? null : e.getUserId().toString())
        .userName(e.getUserName())
        .userRoles(e.getUserRoles())
        .namespace(e.getNamespace())
        .eventType(e.getEventType())
        .operationName(e.getOperationName())
        .operationType(e.getOperationType() == null ? null
            : AuditOperationType.valueOf(e.getOperationType().name()))
        .targetEntity(e.getTargetEntity())
        .targetEntityId(e.getTargetEntityId() == null ? null : e.getTargetEntityId().toString())
        .outcome(e.getOutcome() == null ? null : AuditOutcome.valueOf(e.getOutcome().name()))
        .errorMessage(e.getErrorMessage())
        .endpoint(e.getEndpoint())
        .ipAddress(e.getIpAddress())
        .userAgent(e.getUserAgent())
        .httpStatus(e.getHttpStatus())
        .durationMs(e.getDurationMs() == null ? 0L : e.getDurationMs())
        .payloadRedacted(e.getPayloadRedacted())
        .hasEncryptedPayload(e.getPayloadEncrypted() != null)
        .payloadBefore(e.getPayloadBefore())
        .hasEncryptedBeforePayload(e.getPayloadBeforeEncrypted() != null)
        .prevHash(e.getPrevHash())
        .rowHash(e.getRowHash())
        .build();
  }

  /**
   * Maps a Spring Data page onto the GraphQL page metadata.
   *
   * @param page the entity page
   * @return the GraphQL page info
   */
  public static PageInfo toPageInfo(Page<?> page) {
    return PageInfo.newBuilder()
        .page(page.getNumber())
        .size(page.getSize())
        .totalPages(page.getTotalPages())
        .totalCount((int) page.getTotalElements())
        .hasNextPage(page.hasNext())
        .hasPreviousPage(page.hasPrevious())
        .build();
  }

  /**
   * Maps facet counts onto the GraphQL facet values.
   *
   * @param counts the value, label and count rows
   * @return the GraphQL facet values
   */
  public static List<AuditFacetValue> toFacetValues(List<FacetCount> counts) {
    return counts.stream()
        .map(row -> AuditFacetValue.newBuilder()
            .value(String.valueOf(row.getValue()))
            .label(row.getLabel() == null ? String.valueOf(row.getValue()) : row.getLabel())
            .count(row.getCount())
            .build())
        .toList();
  }
}

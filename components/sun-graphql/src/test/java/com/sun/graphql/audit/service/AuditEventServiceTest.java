package com.sun.graphql.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.base.audit.context.AuditContext;
import com.sun.base.audit.context.AuditRequestSnapshot;
import com.sun.base.audit.context.OperationMetadata;
import com.sun.base.audit.entity.AuditEvent;
import com.sun.base.audit.enums.AuditOutcome;
import com.sun.base.audit.enums.OperationType;
import com.sun.base.audit.redaction.PayloadRedactor;
import com.sun.base.audit.repository.AuditEventRepository;
import com.sun.graphql.audit.config.AuditProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

  @Mock private AuditEventRepository repository;

  private AuditEventService service(boolean enabled) {
    return new AuditEventService(
        repository,
        new PayloadRedactor(new ObjectMapper()),
        new ObjectMapper(),
        new AuditProperties(enabled, null));
  }

  private AuditContext.AuditOperation op(String name, Object variables) {
    return new AuditContext.AuditOperation(
        name,
        new OperationMetadata(name.toUpperCase(), OperationType.MUTATION, "HADES", null, java.util.Set.of()),
        variables,
        null,
        AuditOutcome.SUCCESS,
        null);
  }

  private AuditRequestSnapshot snapshot(UUID userId, List<AuditContext.AuditOperation> ops) {
    return new AuditRequestSnapshot(ops, UUID.randomUUID(), userId, "/graphql", "127.0.0.1", "ua", 200);
  }

  @Test
  void persist_savesBatchOfEvents() {
    AuditEventService svc = service(true);

    svc.persist(snapshot(UUID.randomUUID(), List.of(op("createAnnotation", Map.of()), op("vote", Map.of()))), 12L);

    ArgumentCaptor<List<AuditEvent>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captor.capture());
    List<AuditEvent> batch = captor.getValue();
    assertThat(batch).hasSize(2);
    assertThat(batch.get(0).getRowHash()).isNotBlank();
    assertThat(batch.get(1).getRowHash()).isNotBlank();
  }

  @Test
  void persist_skipsWhenDisabled() {
    AuditEventService svc = service(false);

    svc.persist(snapshot(UUID.randomUUID(), List.of(op("createAnnotation", Map.of()))), 1L);

    verify(repository, never()).saveAll(any());
  }

  @Test
  void persist_recordsNullUserIdForAnonymousRequest() {
    AuditEventService svc = service(true);

    svc.persist(snapshot(null, List.of(op("createAnnotation", Map.of()))), 1L);

    ArgumentCaptor<List<AuditEvent>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captor.capture());
    AuditEvent event = captor.getValue().get(0);
    assertThat(event.getUserId()).isNull();
    assertThat(event.getNamespace()).isEqualTo("HADES");
  }

  @Test
  void persist_redactsSecretsFromPayload() {
    AuditEventService svc = service(true);

    svc.persist(snapshot(UUID.randomUUID(),
        List.of(op("createAnnotation", Map.of("input", Map.of("password", "hunter2"))))), 1L);

    ArgumentCaptor<List<AuditEvent>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captor.capture());
    String payload = captor.getValue().get(0).getPayloadRedacted();
    assertThat(payload).contains("REDACTED");
    assertThat(payload).doesNotContain("hunter2");
  }

  @Test
  void persist_swallowsPersistenceFailures() {
    org.mockito.Mockito.when(repository.saveAll(any())).thenThrow(new RuntimeException("db down"));
    AuditEventService svc = service(true);

    svc.persist(snapshot(UUID.randomUUID(), List.of(op("createAnnotation", Map.of()))), 1L);
  }
}

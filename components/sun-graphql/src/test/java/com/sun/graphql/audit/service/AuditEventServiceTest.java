package com.sun.graphql.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

  @Mock private AuditEventRepository repository;

  private AuditEventService service(boolean enabled, String chainKey) {
    return new AuditEventService(
        repository,
        new PayloadRedactor(new ObjectMapper()),
        new ObjectMapper(),
        new AuditProperties(enabled, new AuditProperties.Chain(chainKey)));
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
  void persist_chainsHashesAcrossOperationsInOneRequest() {
    when(repository.findFirstByOrderByCreatedAtDescIdDesc()).thenReturn(Optional.empty());
    AuditEventService svc = service(true, null);

    svc.persist(snapshot(UUID.randomUUID(), List.of(op("createAnnotation", Map.of()), op("vote", Map.of()))), 12L);

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(repository, times(2)).save(captor.capture());
    AuditEvent first = captor.getAllValues().get(0);
    AuditEvent second = captor.getAllValues().get(1);

    assertThat(first.getPrevHash()).isNull();
    assertThat(first.getRowHash()).matches("[0-9a-f]{64}");
    assertThat(second.getPrevHash()).isEqualTo(first.getRowHash());
  }

  @Test
  void persist_skipsWhenDisabled() {
    AuditEventService svc = service(false, null);

    svc.persist(snapshot(UUID.randomUUID(), List.of(op("createAnnotation", Map.of()))), 1L);

    verify(repository, never()).save(any());
  }

  @Test
  void persist_recordsNullUserIdForAnonymousRequest() {
    when(repository.findFirstByOrderByCreatedAtDescIdDesc()).thenReturn(Optional.empty());
    AuditEventService svc = service(true, null);

    svc.persist(snapshot(null, List.of(op("createAnnotation", Map.of()))), 1L);

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getUserId()).isNull();
    assertThat(captor.getValue().getNamespace()).isEqualTo("HADES");
  }

  @Test
  void persist_redactsSecretsFromPayload() {
    when(repository.findFirstByOrderByCreatedAtDescIdDesc()).thenReturn(Optional.empty());
    AuditEventService svc = service(true, null);

    svc.persist(snapshot(UUID.randomUUID(),
        List.of(op("createAnnotation", Map.of("input", Map.of("password", "hunter2"))))), 1L);

    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(repository).save(captor.capture());
    String payload = captor.getValue().getPayloadRedacted();
    assertThat(payload).contains("REDACTED");
    assertThat(payload).doesNotContain("hunter2");
  }

  @Test
  void persist_swallowsPersistenceFailures() {
    when(repository.findFirstByOrderByCreatedAtDescIdDesc()).thenReturn(Optional.empty());
    when(repository.save(any())).thenThrow(new RuntimeException("db down"));
    AuditEventService svc = service(true, null);

    // Must not propagate - auditing never breaks the response.
    svc.persist(snapshot(UUID.randomUUID(), List.of(op("createAnnotation", Map.of()))), 1L);
  }
}

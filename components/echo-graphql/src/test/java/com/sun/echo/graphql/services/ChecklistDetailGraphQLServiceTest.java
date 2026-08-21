package com.sun.echo.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.codegen.types.RemoteObjectReference;
import com.sun.echo.codegen.types.RemoteObjectType;
import com.sun.echo.service.ChecklistDetailService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistDetailGraphQLServiceTest {

  @Mock private ChecklistDetailService detailService;

  @InjectMocks private ChecklistDetailGraphQLService service;

  @Test
  void locateRemoteObjects_delegatesAndMaps() {
    List<String> ids = List.of("obj-1");
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    com.sun.echo.service.RemoteObjectReference ref =
        new com.sun.echo.service.RemoteObjectReference(id, "ENTRY", ownerId, "desc");
    when(detailService.locateRemoteObjects(ids)).thenReturn(List.of(ref));

    List<RemoteObjectReference> result = service.locateRemoteObjects(ids);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(id.toString());
    assertThat(result.get(0).getOwnerType()).isEqualTo(RemoteObjectType.ENTRY);
    assertThat(result.get(0).getOwnerId()).isEqualTo(ownerId.toString());
    verify(detailService).locateRemoteObjects(ids);
  }

  @Test
  void attachObject_delegatesToService() {
    UUID source = UUID.randomUUID();
    UUID attached = UUID.randomUUID();
    when(detailService.attach(source, "target", "ENTRY")).thenReturn(attached);

    QueryResult result = service.attachObject(source.toString(), "target", RemoteObjectType.ENTRY);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    verify(detailService).attach(source, "target", "ENTRY");
  }

  @Test
  void detachObject_delegatesToService() {
    UUID source = UUID.randomUUID();
    UUID detached = UUID.randomUUID();
    when(detailService.detach(source, "target", "ITEM")).thenReturn(detached);

    QueryResult result = service.detachObject(source.toString(), "target", RemoteObjectType.ITEM);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    verify(detailService).detach(source, "target", "ITEM");
  }

  @Test
  void attachObject_withNullOwnerTypePassesNull() {
    UUID source = UUID.randomUUID();
    UUID attached = UUID.randomUUID();
    when(detailService.attach(source, "target", null)).thenReturn(attached);

    QueryResult result = service.attachObject(source.toString(), "target", null);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    verify(detailService).attach(source, "target", null);
  }
}

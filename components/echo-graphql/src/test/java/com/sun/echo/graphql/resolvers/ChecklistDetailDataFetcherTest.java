package com.sun.echo.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.codegen.types.RemoteObjectReference;
import com.sun.echo.codegen.types.RemoteObjectType;
import com.sun.echo.graphql.services.ChecklistDetailGraphQLService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistDetailDataFetcherTest {

  @Mock private ChecklistDetailGraphQLService checklistDetailGraphQLService;
  @InjectMocks private ChecklistDetailDataFetcher fetcher;

  @Test
  void locateRemoteObjects_delegatesToService() {
    List<String> ids = List.of("obj-1");
    List<RemoteObjectReference> expected = List.of(
        RemoteObjectReference.newBuilder().id(UUID.randomUUID().toString()).ownerType(RemoteObjectType.ENTRY).ownerId(UUID.randomUUID().toString()).build());
    when(checklistDetailGraphQLService.locateRemoteObjects(ids)).thenReturn(expected);

    List<RemoteObjectReference> result = fetcher.locateRemoteObjects(ids);

    assertThat(result).isEqualTo(expected);
    verify(checklistDetailGraphQLService).locateRemoteObjects(ids);
  }

  @Test
  void attachObject_delegatesToService() {
    String source = UUID.randomUUID().toString();
    String target = "target-1";
    QuerySuccess expected = QuerySuccess.newBuilder().message("attachObject succeeded").id(source).build();
    when(checklistDetailGraphQLService.attachObject(source, target, RemoteObjectType.ENTRY)).thenReturn(expected);

    var result = fetcher.attachObject(source, target, RemoteObjectType.ENTRY);

    assertThat(result).isEqualTo(expected);
    verify(checklistDetailGraphQLService).attachObject(source, target, RemoteObjectType.ENTRY);
  }

  @Test
  void detachObject_delegatesToService() {
    String source = UUID.randomUUID().toString();
    String target = "target-1";
    QuerySuccess expected = QuerySuccess.newBuilder().message("detachObject succeeded").id(source).build();
    when(checklistDetailGraphQLService.detachObject(source, target, RemoteObjectType.ITEM)).thenReturn(expected);

    var result = fetcher.detachObject(source, target, RemoteObjectType.ITEM);

    assertThat(result).isEqualTo(expected);
    verify(checklistDetailGraphQLService).detachObject(source, target, RemoteObjectType.ITEM);
  }
}

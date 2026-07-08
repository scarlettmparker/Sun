package com.sun.icarus.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.icarus.codegen.types.CreateThreadInput;
import com.sun.icarus.codegen.types.ForumThread;
import com.sun.icarus.codegen.types.QueryResult;
import com.sun.icarus.codegen.types.QuerySuccess;
import com.sun.icarus.graphql.services.IcarusGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IcarusDataFetcherTest {

  @Mock
  private IcarusGraphQLService service;

  @InjectMocks
  private IcarusDataFetcher fetcher;

  @Test
  void threadsFor_shouldDelegateToService() {
    ForumThread thread = ForumThread.newBuilder().id("1").title("Thread").build();
    when(service.threadsFor("hades:annotation:abc")).thenReturn(
        com.sun.icarus.codegen.types.PagedForumThreads.newBuilder()
            .items(List.of(thread)).pageInfo(
                com.sun.icarus.codegen.types.PageInfo.newBuilder()
                    .page(0).size(1).totalPages(1).totalCount(1)
                    .hasNextPage(false).hasPreviousPage(false).build())
            .build());

    List<ForumThread> result = fetcher.threadsFor("hades:annotation:abc").getItems();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Thread");
  }

  @Test
  void createThread_shouldDelegateToService() {
    CreateThreadInput input = CreateThreadInput.newBuilder()
        .title("Thread").remoteObject("hades:annotation:abc").build();
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").build();
    when(service.createThread(input)).thenReturn(mockResult);

    QueryResult result = fetcher.createThread(input);

    assertThat(result).isEqualTo(mockResult);
  }
}

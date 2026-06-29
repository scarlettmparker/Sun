package com.sun.echo.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.ChecklistItemPage;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.graphql.services.ChecklistGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistDataFetcherTest {

  @Mock private ChecklistGraphQLService service;

  @InjectMocks private ChecklistDataFetcher fetcher;

  @Test
  void items_delegatesToService() {
    ChecklistItemPage page = ChecklistItemPage.newBuilder()
        .items(List.of()).totalCount(0).page(0).size(20).totalPages(0).build();
    when(service.items(0, 20, "createdAt", "ASC")).thenReturn(page);

    assertThat(fetcher.items(0, 20, "createdAt", "ASC")).isEqualTo(page);
  }

  @Test
  void createItem_delegatesToService() {
    QuerySuccess success = QuerySuccess.newBuilder().message("ok").build();
    when(service.createItem("name", null, null)).thenReturn(success);

    QueryResult result = fetcher.createItem("name", null, null);

    assertThat(result).isSameAs(success);
  }
}

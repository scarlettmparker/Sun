package com.sun.echo.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.PagedChecklistItems;
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
    PagedChecklistItems page = PagedChecklistItems.newBuilder().items(List.of()).build();
    when(service.items(any())).thenReturn(page);

    assertThat(fetcher.items(null)).isEqualTo(page);
  }

  @Test
  void createItem_delegatesToService() {
    QuerySuccess success = QuerySuccess.newBuilder().message("ok").build();
    when(service.createItem("name", null, null)).thenReturn(success);

    QueryResult result = fetcher.createItem("name", null, null);

    assertThat(result).isSameAs(success);
  }

  @Test
  void completeChecklist_delegatesToService() {
    QuerySuccess success = QuerySuccess.newBuilder().message("ok").build();
    when(service.completeChecklist("id-1")).thenReturn(success);

    QueryResult result = fetcher.completeChecklist("id-1");

    assertThat(result).isSameAs(success);
  }

  @Test
  void createChecklistFromTemplates_delegatesToService() {
    QuerySuccess success = QuerySuccess.newBuilder().message("ok").build();
    List<String> templateIds = List.of("t-1", "t-2");
    when(service.createChecklistFromTemplates(templateIds)).thenReturn(success);

    QueryResult result = fetcher.createChecklistFromTemplates(templateIds);

    assertThat(result).isSameAs(success);
  }
}

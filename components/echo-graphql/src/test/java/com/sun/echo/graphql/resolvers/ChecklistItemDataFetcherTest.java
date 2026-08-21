package com.sun.echo.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.ChecklistCategory;
import com.sun.echo.codegen.types.ChecklistCategoryInput;
import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistItem;
import com.sun.echo.codegen.types.ChecklistItemInput;
import com.sun.echo.codegen.types.PagedChecklistItems;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.graphql.services.ChecklistItemGraphQLService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistItemDataFetcherTest {

  @Mock private ChecklistItemGraphQLService checklistItemGraphQLService;
  @InjectMocks private ChecklistItemDataFetcher fetcher;

  @Test
  void items_delegatesToService() {
    PaginationInput pagination = PaginationInput.newBuilder().page(0).size(10).build();
    PagedChecklistItems expected = PagedChecklistItems.newBuilder().items(List.of()).build();
    when(checklistItemGraphQLService.items(pagination)).thenReturn(expected);

    PagedChecklistItems result = fetcher.items(pagination);

    assertThat(result).isEqualTo(expected);
    verify(checklistItemGraphQLService).items(pagination);
  }

  @Test
  void item_delegatesToService() {
    String id = UUID.randomUUID().toString();
    ChecklistItem expected = ChecklistItem.newBuilder().id(id).name("x").build();
    when(checklistItemGraphQLService.item(id)).thenReturn(expected);

    ChecklistItem result = fetcher.item(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistItemGraphQLService).item(id);
  }

  @Test
  void itemDetails_delegatesToService() {
    String id = UUID.randomUUID().toString();
    ChecklistDetail expected = ChecklistDetail.newBuilder().ownerId(id).description("d").build();
    when(checklistItemGraphQLService.itemDetails(id)).thenReturn(expected);

    ChecklistDetail result = fetcher.itemDetails(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistItemGraphQLService).itemDetails(id);
  }

  @Test
  void listCategories_delegatesToService() {
    List<ChecklistCategory> expected = List.of(ChecklistCategory.newBuilder().id(UUID.randomUUID().toString()).name("c").build());
    when(checklistItemGraphQLService.listCategories()).thenReturn(expected);

    List<ChecklistCategory> result = fetcher.listCategories();

    assertThat(result).isEqualTo(expected);
    verify(checklistItemGraphQLService).listCategories();
  }

  @Test
  void createItem_delegatesToService() {
    QuerySuccess expected = QuerySuccess.newBuilder().message("createItem succeeded").id(UUID.randomUUID().toString()).build();
    when(checklistItemGraphQLService.createItem("n", "d", null, "icon")).thenReturn(expected);

    var result = fetcher.createItem("n", "d", null, "icon");

    assertThat(result).isEqualTo(expected);
    verify(checklistItemGraphQLService).createItem("n", "d", null, "icon");
  }

  @Test
  void saveItem_delegatesToService() {
    ChecklistItemInput input = ChecklistItemInput.newBuilder().name("x").build();
    QuerySuccess expected = QuerySuccess.newBuilder().message("saveItem succeeded").id(UUID.randomUUID().toString()).build();
    when(checklistItemGraphQLService.saveItem(input)).thenReturn(expected);

    var result = fetcher.saveItem(input);

    assertThat(result).isEqualTo(expected);
    verify(checklistItemGraphQLService).saveItem(input);
  }

  @Test
  void retireItem_delegatesToService() {
    String id = UUID.randomUUID().toString();
    QuerySuccess expected = QuerySuccess.newBuilder().message("retireItem succeeded").id(id).build();
    when(checklistItemGraphQLService.retireItem(id)).thenReturn(expected);

    var result = fetcher.retireItem(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistItemGraphQLService).retireItem(id);
  }

  @Test
  void createCategory_delegatesToService() {
    QuerySuccess expected = QuerySuccess.newBuilder().message("createCategory succeeded").id(UUID.randomUUID().toString()).build();
    when(checklistItemGraphQLService.createCategory("cat", "desc")).thenReturn(expected);

    var result = fetcher.createCategory("cat", "desc");

    assertThat(result).isEqualTo(expected);
    verify(checklistItemGraphQLService).createCategory("cat", "desc");
  }

  @Test
  void saveCategory_delegatesToService() {
    ChecklistCategoryInput input = ChecklistCategoryInput.newBuilder().name("cat").build();
    QuerySuccess expected = QuerySuccess.newBuilder().message("saveCategory succeeded").id(UUID.randomUUID().toString()).build();
    when(checklistItemGraphQLService.saveCategory(input)).thenReturn(expected);

    var result = fetcher.saveCategory(input);

    assertThat(result).isEqualTo(expected);
    verify(checklistItemGraphQLService).saveCategory(input);
  }
}

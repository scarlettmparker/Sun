package com.sun.echo.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistTemplate;
import com.sun.echo.codegen.types.ChecklistTemplateInput;
import com.sun.echo.codegen.types.PagedChecklistTemplateItems;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.graphql.services.ChecklistTemplateGraphQLService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistTemplateDataFetcherTest {

  @Mock private ChecklistTemplateGraphQLService checklistTemplateGraphQLService;
  @InjectMocks private ChecklistTemplateDataFetcher fetcher;

  @Test
  void template_delegatesToService() {
    String id = UUID.randomUUID().toString();
    ChecklistTemplate expected = ChecklistTemplate.newBuilder().id(id).name("t").build();
    when(checklistTemplateGraphQLService.template(id)).thenReturn(expected);

    ChecklistTemplate result = fetcher.template(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistTemplateGraphQLService).template(id);
  }

  @Test
  void templateDetails_delegatesToService() {
    String id = UUID.randomUUID().toString();
    ChecklistDetail expected = ChecklistDetail.newBuilder().ownerId(id).description("d").build();
    when(checklistTemplateGraphQLService.templateDetails(id)).thenReturn(expected);

    ChecklistDetail result = fetcher.templateDetails(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistTemplateGraphQLService).templateDetails(id);
  }

  @Test
  void listTemplates_delegatesToService() {
    List<ChecklistTemplate> expected = List.of(ChecklistTemplate.newBuilder().id(UUID.randomUUID().toString()).name("t").build());
    when(checklistTemplateGraphQLService.listTemplates()).thenReturn(expected);

    List<ChecklistTemplate> result = fetcher.listTemplates();

    assertThat(result).isEqualTo(expected);
    verify(checklistTemplateGraphQLService).listTemplates();
  }

  @Test
  void templateItems_delegatesToService() {
    String templateId = UUID.randomUUID().toString();
    PaginationInput pagination = PaginationInput.newBuilder().page(0).size(10).build();
    PagedChecklistTemplateItems expected = PagedChecklistTemplateItems.newBuilder().items(List.of()).build();
    when(checklistTemplateGraphQLService.templateItems(templateId, pagination)).thenReturn(expected);

    PagedChecklistTemplateItems result = fetcher.templateItems(templateId, pagination);

    assertThat(result).isEqualTo(expected);
    verify(checklistTemplateGraphQLService).templateItems(templateId, pagination);
  }

  @Test
  void createTemplate_delegatesToService() {
    QuerySuccess expected = QuerySuccess.newBuilder().message("createTemplate succeeded").id(UUID.randomUUID().toString()).build();
    when(checklistTemplateGraphQLService.createTemplate("n", "d", null)).thenReturn(expected);

    var result = fetcher.createTemplate("n", "d", null);

    assertThat(result).isEqualTo(expected);
    verify(checklistTemplateGraphQLService).createTemplate("n", "d", null);
  }

  @Test
  void saveTemplate_delegatesToService() {
    ChecklistTemplateInput input = ChecklistTemplateInput.newBuilder().name("t").build();
    QuerySuccess expected = QuerySuccess.newBuilder().message("saveTemplate succeeded").id(UUID.randomUUID().toString()).build();
    when(checklistTemplateGraphQLService.saveTemplate(input)).thenReturn(expected);

    var result = fetcher.saveTemplate(input);

    assertThat(result).isEqualTo(expected);
    verify(checklistTemplateGraphQLService).saveTemplate(input);
  }

  @Test
  void archiveTemplate_delegatesToService() {
    String id = UUID.randomUUID().toString();
    QuerySuccess expected = QuerySuccess.newBuilder().message("archiveTemplate succeeded").id(id).build();
    when(checklistTemplateGraphQLService.archiveTemplate(id)).thenReturn(expected);

    var result = fetcher.archiveTemplate(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistTemplateGraphQLService).archiveTemplate(id);
  }

  @Test
  void addTemplateItem_delegatesToService() {
    String templateId = UUID.randomUUID().toString();
    String itemId = UUID.randomUUID().toString();
    QuerySuccess expected = QuerySuccess.newBuilder().message("addTemplateItem succeeded").id(UUID.randomUUID().toString()).build();
    when(checklistTemplateGraphQLService.addTemplateItem(templateId, itemId, 1)).thenReturn(expected);

    var result = fetcher.addTemplateItem(templateId, itemId, 1);

    assertThat(result).isEqualTo(expected);
    verify(checklistTemplateGraphQLService).addTemplateItem(templateId, itemId, 1);
  }

  @Test
  void removeTemplateItem_delegatesToService() {
    String templateId = UUID.randomUUID().toString();
    String itemId = UUID.randomUUID().toString();
    QuerySuccess expected = QuerySuccess.newBuilder().message("removeTemplateItem succeeded").id(templateId).build();
    when(checklistTemplateGraphQLService.removeTemplateItem(templateId, itemId)).thenReturn(expected);

    var result = fetcher.removeTemplateItem(templateId, itemId);

    assertThat(result).isEqualTo(expected);
    verify(checklistTemplateGraphQLService).removeTemplateItem(templateId, itemId);
  }
}

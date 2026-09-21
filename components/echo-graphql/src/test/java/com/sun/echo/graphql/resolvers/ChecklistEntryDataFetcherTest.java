package com.sun.echo.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.AddItemResponse;
import com.sun.echo.codegen.types.ArchiveChecklistResponse;
import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistEntry;
import com.sun.echo.codegen.types.ChecklistEntryInput;
import com.sun.echo.codegen.types.CompleteChecklistResponse;
import com.sun.echo.codegen.types.CreateChecklistFromTemplateResponse;
import com.sun.echo.codegen.types.CreateChecklistFromTemplatesResponse;
import com.sun.echo.codegen.types.CreateChecklistResponse;
import com.sun.echo.codegen.types.DeleteChecklistResponse;
import com.sun.echo.codegen.types.PagedChecklistEntryItems;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.RemoveItemResponse;
import com.sun.echo.codegen.types.SaveChecklistResponse;
import com.sun.echo.codegen.types.SetItemStatusResponse;
import com.sun.echo.graphql.services.ChecklistEntryGraphQLService;
import com.sun.echo.model.enums.ItemStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistEntryDataFetcherTest {

  @Mock private ChecklistEntryGraphQLService checklistEntryGraphQLService;
  @InjectMocks private ChecklistEntryDataFetcher fetcher;

  @Test
  void entry_delegatesToService() {
    String id = UUID.randomUUID().toString();
    ChecklistEntry expected = ChecklistEntry.newBuilder().id(id).name("e").build();
    when(checklistEntryGraphQLService.entry(id)).thenReturn(expected);

    ChecklistEntry result = fetcher.entry(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).entry(id);
  }

  @Test
  void entryDetails_delegatesToService() {
    String id = UUID.randomUUID().toString();
    ChecklistDetail expected = ChecklistDetail.newBuilder().ownerId(id).description("d").build();
    when(checklistEntryGraphQLService.entryDetails(id)).thenReturn(expected);

    ChecklistDetail result = fetcher.entryDetails(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).entryDetails(id);
  }

  @Test
  void entryItems_delegatesToService() {
    String entryId = UUID.randomUUID().toString();
    PaginationInput pagination = PaginationInput.newBuilder().page(0).size(10).build();
    PagedChecklistEntryItems expected = PagedChecklistEntryItems.newBuilder().items(List.of()).build();
    when(checklistEntryGraphQLService.entryItems(entryId, pagination)).thenReturn(expected);

    PagedChecklistEntryItems result = fetcher.entryItems(entryId, pagination);

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).entryItems(entryId, pagination);
  }

  @Test
  void listEntries_delegatesToService() {
    List<ChecklistEntry> expected = List.of(ChecklistEntry.newBuilder().id(UUID.randomUUID().toString()).name("e").build());
    when(checklistEntryGraphQLService.listEntries()).thenReturn(expected);

    List<ChecklistEntry> result = fetcher.listEntries();

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).listEntries();
  }

  @Test
  void createChecklist_delegatesToService() {
    CreateChecklistResponse expected = CreateChecklistResponse.newBuilder().message("Checklist created successfully")
        .entry(ChecklistEntry.newBuilder().id(UUID.randomUUID().toString()).name("name").build()).build();
    when(checklistEntryGraphQLService.createChecklist("name")).thenReturn(expected);

    var result = fetcher.createChecklist("name");

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).createChecklist("name");
  }

  @Test
  void createChecklistFromTemplate_delegatesToService() {
    String templateId = UUID.randomUUID().toString();
    CreateChecklistFromTemplateResponse expected = CreateChecklistFromTemplateResponse.newBuilder()
        .message("Checklist created from template successfully")
        .entry(ChecklistEntry.newBuilder().id(UUID.randomUUID().toString()).name("n").build()).build();
    when(checklistEntryGraphQLService.createChecklistFromTemplate(templateId, "n")).thenReturn(expected);

    var result = fetcher.createChecklistFromTemplate(templateId, "n");

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).createChecklistFromTemplate(templateId, "n");
  }

  @Test
  void createChecklistFromTemplates_delegatesToService() {
    List<String> ids = List.of(UUID.randomUUID().toString());
    CreateChecklistFromTemplatesResponse expected = CreateChecklistFromTemplatesResponse.newBuilder()
        .message("Checklist created from templates successfully")
        .entry(ChecklistEntry.newBuilder().id(UUID.randomUUID().toString()).name("n").build()).build();
    when(checklistEntryGraphQLService.createChecklistFromTemplates(ids, "n")).thenReturn(expected);

    var result = fetcher.createChecklistFromTemplates(ids, "n");

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).createChecklistFromTemplates(ids, "n");
  }

  @Test
  void saveChecklist_delegatesToService() {
    ChecklistEntryInput input = ChecklistEntryInput.newBuilder().name("e").build();
    SaveChecklistResponse expected = SaveChecklistResponse.newBuilder().message("Checklist saved successfully")
        .entry(ChecklistEntry.newBuilder().id(UUID.randomUUID().toString()).name("e").build()).build();
    when(checklistEntryGraphQLService.saveChecklist(input)).thenReturn(expected);

    var result = fetcher.saveChecklist(input);

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).saveChecklist(input);
  }

  @Test
  void completeChecklist_delegatesToService() {
    String id = UUID.randomUUID().toString();
    CompleteChecklistResponse expected = CompleteChecklistResponse.newBuilder().message("Checklist completed successfully")
        .entry(ChecklistEntry.newBuilder().id(id).name("e").build()).build();
    when(checklistEntryGraphQLService.completeChecklist(id)).thenReturn(expected);

    var result = fetcher.completeChecklist(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).completeChecklist(id);
  }

  @Test
  void archiveChecklist_delegatesToService() {
    String id = UUID.randomUUID().toString();
    ArchiveChecklistResponse expected = ArchiveChecklistResponse.newBuilder().message("Checklist archived successfully")
        .entry(ChecklistEntry.newBuilder().id(id).name("e").build()).build();
    when(checklistEntryGraphQLService.archiveChecklist(id)).thenReturn(expected);

    var result = fetcher.archiveChecklist(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).archiveChecklist(id);
  }

  @Test
  void deleteChecklist_delegatesToService() {
    String id = UUID.randomUUID().toString();
    DeleteChecklistResponse expected = DeleteChecklistResponse.newBuilder().message("Checklist deleted successfully").id(id).build();
    when(checklistEntryGraphQLService.deleteChecklist(id)).thenReturn(expected);

    var result = fetcher.deleteChecklist(id);

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).deleteChecklist(id);
  }

  @Test
  void addItem_delegatesToService() {
    String entryId = UUID.randomUUID().toString();
    String itemId = UUID.randomUUID().toString();
    AddItemResponse expected = AddItemResponse.newBuilder().message("Checklist item added successfully")
        .entry(ChecklistEntry.newBuilder().id(entryId).name("e").build()).build();
    when(checklistEntryGraphQLService.addItem(entryId, itemId, 0)).thenReturn(expected);

    var result = fetcher.addItem(entryId, itemId, 0);

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).addItem(entryId, itemId, 0);
  }

  @Test
  void removeItem_delegatesToService() {
    String entryId = UUID.randomUUID().toString();
    String itemId = UUID.randomUUID().toString();
    RemoveItemResponse expected = RemoveItemResponse.newBuilder().message("Checklist item removed successfully")
        .entry(ChecklistEntry.newBuilder().id(entryId).name("e").build()).build();
    when(checklistEntryGraphQLService.removeItem(entryId, itemId)).thenReturn(expected);

    var result = fetcher.removeItem(entryId, itemId);

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).removeItem(entryId, itemId);
  }

  @Test
  void setItemStatus_delegatesToService() {
    String entryId = UUID.randomUUID().toString();
    String itemId = UUID.randomUUID().toString();
    SetItemStatusResponse expected = SetItemStatusResponse.newBuilder()
        .message("Checklist item status updated successfully")
        .entry(ChecklistEntry.newBuilder().id(entryId).name("e").build()).build();
    when(checklistEntryGraphQLService.setItemStatus(entryId, itemId, ItemStatus.COMPLETE)).thenReturn(expected);

    var result = fetcher.setItemStatus(entryId, itemId, ItemStatus.COMPLETE);

    assertThat(result).isEqualTo(expected);
    verify(checklistEntryGraphQLService).setItemStatus(entryId, itemId, ItemStatus.COMPLETE);
  }
}

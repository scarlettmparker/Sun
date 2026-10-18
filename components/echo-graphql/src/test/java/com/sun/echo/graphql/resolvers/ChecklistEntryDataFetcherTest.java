package com.sun.echo.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.base.error.MutationException;
import com.sun.echo.codegen.types.AddItemResponse;
import com.sun.echo.codegen.types.ArchiveChecklistResponse;
import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistEntry;
import com.sun.echo.codegen.types.ChecklistEntryInput;
import com.sun.echo.codegen.types.ChecklistEntryItem;
import com.sun.echo.codegen.types.CompleteChecklistResponse;
import com.sun.echo.codegen.types.CreateChecklistFromTemplateResponse;
import com.sun.echo.codegen.types.CreateChecklistFromTemplatesResponse;
import com.sun.echo.codegen.types.CreateChecklistResponse;
import com.sun.echo.codegen.types.DeleteChecklistResponse;
import com.sun.echo.codegen.types.PagedChecklistEntryItems;
import com.sun.echo.codegen.types.RemoveItemResponse;
import com.sun.echo.codegen.types.SaveChecklistResponse;
import com.sun.echo.codegen.types.SetItemStatusResponse;
import com.sun.echo.graphql.mappers.ChecklistDetailMapper;
import com.sun.echo.graphql.mappers.ChecklistEntryItemMapper;
import com.sun.echo.graphql.mappers.ChecklistEntryMapper;
import com.sun.echo.model.ChecklistEntryDetailEntity;
import com.sun.echo.model.ChecklistEntryEntity;
import com.sun.echo.model.ChecklistEntryItemEntity;
import com.sun.echo.model.enums.ItemStatus;
import com.sun.echo.service.ChecklistDetailService;
import com.sun.echo.service.ChecklistEntryItemService;
import com.sun.echo.service.ChecklistEntryService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ChecklistEntryDataFetcherTest {

  @Mock private ChecklistEntryService entryService;
  @Mock private ChecklistEntryItemService entryItemService;
  @Mock private ChecklistDetailService detailService;
  @Mock private ChecklistEntryMapper entryMapper;
  @Mock private ChecklistEntryItemMapper entryItemMapper;
  @Mock private ChecklistDetailMapper detailMapper;

  @InjectMocks private ChecklistEntryDataFetcher fetcher;

  @Test
  void entry_delegatesToLocateAndMap() {
    UUID id = UUID.randomUUID();
    ChecklistEntryEntity entity = new ChecklistEntryEntity();
    entity.setId(id);
    when(entryService.locate(id)).thenReturn(Optional.of(entity));
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(id.toString()).name("e").build();
    when(entryMapper.map(entity)).thenReturn(mapped);

    ChecklistEntry result = fetcher.entry(id.toString());

    assertThat(result).isEqualTo(mapped);
    verify(entryService).locate(id);
  }

  @Test
  void entryDetails_delegatesToFindAndMap() {
    UUID id = UUID.randomUUID();
    ChecklistEntryDetailEntity detail = new ChecklistEntryDetailEntity();
    when(detailService.findEntryDetail(id)).thenReturn(Optional.of(detail));
    ChecklistDetail mapped = ChecklistDetail.newBuilder().ownerId(id.toString()).description("d").build();
    when(detailMapper.map(detail)).thenReturn(mapped);

    ChecklistDetail result = fetcher.entryDetails(id.toString());

    assertThat(result).isEqualTo(mapped);
    verify(detailService).findEntryDetail(id);
  }

  @Test
  void entryItems_delegatesToPagedQueryAndMapper() {
    UUID entryId = UUID.randomUUID();
    ChecklistEntryItemEntity entity = new ChecklistEntryItemEntity();
    entity.setId(UUID.randomUUID());
    Page<ChecklistEntryItemEntity> page = new PageImpl<>(List.of(entity));
    when(entryItemService.listForEntryPaged(eq(entryId), any(Pageable.class))).thenReturn(page);
    ChecklistEntryItem mapped = ChecklistEntryItem.newBuilder().id(entity.getId().toString()).build();
    when(entryItemMapper.map(anyList())).thenReturn(List.of(mapped));

    PagedChecklistEntryItems result = fetcher.entryItems(entryId.toString(), null);

    assertThat(result.getItems()).containsExactly(mapped);
    assertThat(result.getPageInfo().getTotalCount()).isEqualTo(1);
    verify(entryItemService).listForEntryPaged(eq(entryId), any(Pageable.class));
  }

  @Test
  void listEntries_delegatesToService() {
    ChecklistEntryEntity entity = new ChecklistEntryEntity();
    entity.setId(UUID.randomUUID());
    when(entryService.findAll()).thenReturn(List.of(entity));
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(entity.getId().toString()).name("e").build();
    when(entryMapper.map(entity)).thenReturn(mapped);

    List<ChecklistEntry> result = fetcher.listEntries();

    assertThat(result).containsExactly(mapped);
    verify(entryService).findAll();
  }

  @Test
  void createChecklist_savesAndReturnsEntry() {
    ChecklistEntryEntity saved = new ChecklistEntryEntity();
    saved.setId(UUID.randomUUID());
    when(entryService.save(any(ChecklistEntryEntity.class))).thenReturn(saved);
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(saved.getId().toString()).name("My checklist").build();
    when(entryMapper.map(saved)).thenReturn(mapped);

    CreateChecklistResponse result = fetcher.createChecklist("My checklist");

    assertThat(result.getEntry()).isEqualTo(mapped);
    assertThat(result.getMessage()).contains("created");
    verify(entryService).save(any(ChecklistEntryEntity.class));
  }

  @Test
  void createChecklist_throwsMutationExceptionWhenSaveFails() {
    when(entryService.save(any(ChecklistEntryEntity.class))).thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> fetcher.createChecklist("My checklist"))
        .isInstanceOf(MutationException.class)
        .hasMessageContaining("createChecklist failed: Database error");
  }

  @Test
  void createChecklistFromTemplate_delegatesToService() {
    UUID templateId = UUID.randomUUID();
    ChecklistEntryEntity created = new ChecklistEntryEntity();
    created.setId(UUID.randomUUID());
    when(entryService.createFromTemplate(templateId, "n")).thenReturn(created);
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(created.getId().toString()).name("n").build();
    when(entryMapper.map(created)).thenReturn(mapped);

    CreateChecklistFromTemplateResponse result = fetcher.createChecklistFromTemplate(templateId.toString(), "n");

    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(entryService).createFromTemplate(templateId, "n");
  }

  @Test
  void createChecklistFromTemplates_delegatesToService() {
    List<String> ids = List.of(UUID.randomUUID().toString());
    ChecklistEntryEntity created = new ChecklistEntryEntity();
    created.setId(UUID.randomUUID());
    when(entryService.createFromTemplates(anyList(), eq("n"))).thenReturn(created);
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(created.getId().toString()).name("n").build();
    when(entryMapper.map(created)).thenReturn(mapped);

    CreateChecklistFromTemplatesResponse result = fetcher.createChecklistFromTemplates(ids, "n");

    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(entryService).createFromTemplates(anyList(), eq("n"));
  }

  @Test
  void saveChecklist_appliesInputAndSaves() {
    ChecklistEntryInput input = ChecklistEntryInput.newBuilder().name("e").build();
    ChecklistEntryEntity saved = new ChecklistEntryEntity();
    saved.setId(UUID.randomUUID());
    when(entryService.save(any(ChecklistEntryEntity.class))).thenReturn(saved);
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(saved.getId().toString()).name("e").build();
    when(entryMapper.map(saved)).thenReturn(mapped);
    doNothing().when(entryMapper).map(eq(input), any(ChecklistEntryEntity.class));

    SaveChecklistResponse result = fetcher.saveChecklist(input);

    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(entryMapper).map(eq(input), any(ChecklistEntryEntity.class));
    verify(entryService).save(any(ChecklistEntryEntity.class));
  }

  @Test
  void completeChecklist_delegatesToService() {
    UUID id = UUID.randomUUID();
    ChecklistEntryEntity completed = new ChecklistEntryEntity();
    completed.setId(id);
    when(entryService.completeChecklist(id)).thenReturn(completed);
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(id.toString()).name("e").build();
    when(entryMapper.map(completed)).thenReturn(mapped);

    CompleteChecklistResponse result = fetcher.completeChecklist(id.toString());

    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(entryService).completeChecklist(id);
  }

  @Test
  void archiveChecklist_delegatesToService() {
    UUID id = UUID.randomUUID();
    ChecklistEntryEntity archived = new ChecklistEntryEntity();
    archived.setId(id);
    when(entryService.archive(id)).thenReturn(archived);
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(id.toString()).name("e").build();
    when(entryMapper.map(archived)).thenReturn(mapped);

    ArchiveChecklistResponse result = fetcher.archiveChecklist(id.toString());

    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(entryService).archive(id);
  }

  @Test
  void deleteChecklist_delegatesToService() {
    UUID id = UUID.randomUUID();

    DeleteChecklistResponse result = fetcher.deleteChecklist(id.toString());

    assertThat(result.getId()).isEqualTo(id.toString());
    verify(entryService).delete(id);
  }

  @Test
  void addItem_delegatesToService() {
    UUID entryId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    ChecklistEntryEntity entry = new ChecklistEntryEntity();
    entry.setId(entryId);
    when(entryService.locate(entryId)).thenReturn(Optional.of(entry));
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(entryId.toString()).name("e").build();
    when(entryMapper.map(entry)).thenReturn(mapped);

    AddItemResponse result = fetcher.addItem(entryId.toString(), itemId.toString(), 0);

    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(entryItemService).addItem(entryId, itemId, 0);
  }

  @Test
  void removeItem_delegatesToService() {
    UUID entryId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    ChecklistEntryEntity entry = new ChecklistEntryEntity();
    entry.setId(entryId);
    when(entryService.locate(entryId)).thenReturn(Optional.of(entry));
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(entryId.toString()).name("e").build();
    when(entryMapper.map(entry)).thenReturn(mapped);

    RemoveItemResponse result = fetcher.removeItem(entryId.toString(), itemId.toString());

    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(entryItemService).removeItem(entryId, itemId);
  }

  @Test
  void setItemStatus_delegatesToService() {
    UUID entryId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    ChecklistEntryEntity entry = new ChecklistEntryEntity();
    entry.setId(entryId);
    when(entryService.locate(entryId)).thenReturn(Optional.of(entry));
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(entryId.toString()).name("e").build();
    when(entryMapper.map(entry)).thenReturn(mapped);

    SetItemStatusResponse result = fetcher.setItemStatus(entryId.toString(), itemId.toString(), ItemStatus.COMPLETE);

    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(entryItemService).setStatus(entryId, itemId, ItemStatus.COMPLETE);
  }
}

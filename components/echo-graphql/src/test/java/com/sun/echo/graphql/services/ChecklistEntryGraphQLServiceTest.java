package com.sun.echo.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.ChecklistEntry;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.graphql.mappers.ChecklistDetailMapper;
import com.sun.echo.graphql.mappers.ChecklistEntryItemMapper;
import com.sun.echo.graphql.mappers.ChecklistEntryMapper;
import com.sun.echo.model.ChecklistEntryEntity;
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

@ExtendWith(MockitoExtension.class)
class ChecklistEntryGraphQLServiceTest {

  @Mock private ChecklistEntryService entryService;
  @Mock private ChecklistEntryItemService entryItemService;
  @Mock private ChecklistDetailService detailService;
  @Mock private ChecklistEntryMapper entryMapper;
  @Mock private ChecklistEntryItemMapper entryItemMapper;
  @Mock private ChecklistDetailMapper detailMapper;

  @InjectMocks private ChecklistEntryGraphQLService service;

  @Test
  void entry_delegatesToLocateAndMap() {
    UUID id = UUID.randomUUID();
    ChecklistEntryEntity entity = new ChecklistEntryEntity();
    entity.setId(id);
    when(entryService.locate(id)).thenReturn(Optional.of(entity));
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(id.toString()).name("e").build();
    when(entryMapper.map(entity)).thenReturn(mapped);

    ChecklistEntry result = service.entry(id.toString());

    assertThat(result).isEqualTo(mapped);
    verify(entryService).locate(id);
  }

  @Test
  void createChecklist_savesAndReturnsSuccess() {
    ChecklistEntryEntity saved = new ChecklistEntryEntity();
    saved.setId(UUID.randomUUID());
    when(entryService.save(any(ChecklistEntryEntity.class))).thenReturn(saved);

    QueryResult result = service.createChecklist("My checklist");

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(saved.getId().toString());
    verify(entryService).save(any(ChecklistEntryEntity.class));
  }

  @Test
  void completeChecklist_delegatesToService() {
    UUID id = UUID.randomUUID();
    ChecklistEntryEntity completed = new ChecklistEntryEntity();
    completed.setId(id);
    when(entryService.completeChecklist(id)).thenReturn(completed);

    QueryResult result = service.completeChecklist(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    verify(entryService).completeChecklist(id);
  }

  @Test
  void listEntries_delegatesToService() {
    ChecklistEntryEntity entity = new ChecklistEntryEntity();
    entity.setId(UUID.randomUUID());
    when(entryService.findAll()).thenReturn(List.of(entity));
    ChecklistEntry mapped = ChecklistEntry.newBuilder().id(entity.getId().toString()).name("e").build();
    when(entryMapper.map(entity)).thenReturn(mapped);

    List<ChecklistEntry> result = service.listEntries();

    assertThat(result).containsExactly(mapped);
    verify(entryService).findAll();
  }

  @Test
  void archiveChecklist_delegatesToService() {
    UUID id = UUID.randomUUID();
    ChecklistEntryEntity archived = new ChecklistEntryEntity();
    archived.setId(id);
    when(entryService.archive(id)).thenReturn(archived);

    QueryResult result = service.archiveChecklist(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    verify(entryService).archive(id);
  }
}

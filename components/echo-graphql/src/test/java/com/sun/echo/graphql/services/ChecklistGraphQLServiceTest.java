package com.sun.echo.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.ChecklistItem;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.codegen.types.RemoteObjectReference;
import com.sun.echo.codegen.types.RemoteObjectType;
import com.sun.echo.graphql.mappers.ChecklistCategoryMapper;
import com.sun.echo.graphql.mappers.ChecklistDetailMapper;
import com.sun.echo.graphql.mappers.ChecklistEntryItemMapper;
import com.sun.echo.graphql.mappers.ChecklistEntryMapper;
import com.sun.echo.graphql.mappers.ChecklistItemMapper;
import com.sun.echo.graphql.mappers.ChecklistTemplateItemMapper;
import com.sun.echo.graphql.mappers.ChecklistTemplateMapper;
import com.sun.echo.model.ChecklistEntryEntity;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.service.ChecklistCategoryService;
import com.sun.echo.service.ChecklistDetailService;
import com.sun.echo.service.ChecklistEntryItemService;
import com.sun.echo.service.ChecklistEntryService;
import com.sun.echo.service.ChecklistItemService;
import com.sun.echo.service.ChecklistTemplateItemService;
import com.sun.echo.service.ChecklistTemplateService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ChecklistGraphQLServiceTest {

  @Mock private ChecklistItemService itemService;
  @Mock private ChecklistCategoryService categoryService;
  @Mock private ChecklistEntryService entryService;
  @Mock private ChecklistTemplateService templateService;
  @Mock private ChecklistEntryItemService entryItemService;
  @Mock private ChecklistTemplateItemService templateItemService;
  @Mock private ChecklistDetailService detailService;

  @Mock private ChecklistItemMapper itemMapper;
  @Mock private ChecklistCategoryMapper categoryMapper;
  @Mock private ChecklistEntryMapper entryMapper;
  @Mock private ChecklistTemplateMapper templateMapper;
  @Mock private ChecklistEntryItemMapper entryItemMapper;
  @Mock private ChecklistTemplateItemMapper templateItemMapper;
  @Mock private ChecklistDetailMapper detailMapper;

  @InjectMocks private ChecklistGraphQLService service;

  @Test
  void items_returnsPagedResult() {
    ChecklistItemEntity entity = new ChecklistItemEntity();
    entity.setId(UUID.randomUUID());
    when(itemService.findAllPaged(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));

    int count = service.items(null).getPageInfo().getTotalCount();

    assertThat(count).isEqualTo(1);
  }

  @Test
  void item_returnsMappedItemWhenFound() {
    UUID id = UUID.randomUUID();
    ChecklistItemEntity entity = new ChecklistItemEntity();
    entity.setId(id);
    when(itemService.locate(id)).thenReturn(Optional.of(entity));
    when(itemMapper.map(entity)).thenReturn(ChecklistItem.newBuilder().id(id.toString()).build());

    assertThat(service.item(id.toString())).isNotNull();
  }

  @Test
  void item_returnsNullWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(itemService.locate(id)).thenReturn(Optional.empty());

    assertThat(service.item(id.toString())).isNull();
  }

  @Test
  void createItem_returnsSuccessWithId() {
    ChecklistItemEntity saved = new ChecklistItemEntity();
    saved.setId(UUID.randomUUID());
    when(itemService.save(any(ChecklistItemEntity.class))).thenReturn(saved);

    QueryResult result = service.createItem("name", "desc", null);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(saved.getId().toString());
  }

  @Test
  void completeChecklist_returnsSuccessWithId() {
    UUID id = UUID.randomUUID();
    ChecklistEntryEntity entry = new ChecklistEntryEntity();
    entry.setId(id);
    when(entryService.completeChecklist(id)).thenReturn(entry);

    QueryResult result = service.completeChecklist(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
  }

  @Test
  void createChecklistFromTemplates_returnsSuccessWithId() {
    UUID id = UUID.randomUUID();
    ChecklistEntryEntity entry = new ChecklistEntryEntity();
    entry.setId(id);
    when(entryService.createFromTemplates(any(List.class))).thenReturn(entry);

    QueryResult result =
        service.createChecklistFromTemplates(List.of(UUID.randomUUID().toString()));

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
  }

  @Test
  void attachObject_returnsDetailId() {
    UUID detailId = UUID.randomUUID();
    when(detailService.attach(any(UUID.class), eq("blog-1"), isNull())).thenReturn(detailId);

    QueryResult result = service.attachObject(UUID.randomUUID().toString(), "blog-1", null);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(detailId.toString());
  }

  @Test
  void locateRemoteObjects_mapsOwnerType() {
    UUID detailId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    com.sun.echo.service.RemoteObjectReference ref =
        new com.sun.echo.service.RemoteObjectReference(detailId, "ENTRY", ownerId, "notes");
    when(detailService.locateRemoteObjects(List.of("blog-1"))).thenReturn(List.of(ref));

    List<RemoteObjectReference> result = service.locateRemoteObjects(List.of("blog-1"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOwnerType()).isEqualTo(RemoteObjectType.ENTRY);
    assertThat(result.get(0).getOwnerId()).isEqualTo(ownerId.toString());
  }
}

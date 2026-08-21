package com.sun.echo.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.echo.codegen.types.ChecklistCategory;
import com.sun.echo.codegen.types.ChecklistItem;
import com.sun.echo.codegen.types.PagedChecklistItems;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.graphql.mappers.ChecklistCategoryMapper;
import com.sun.echo.graphql.mappers.ChecklistDetailMapper;
import com.sun.echo.graphql.mappers.ChecklistItemMapper;
import com.sun.echo.model.ChecklistCategoryEntity;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.service.ChecklistCategoryService;
import com.sun.echo.service.ChecklistDetailService;
import com.sun.echo.service.ChecklistItemService;
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
class ChecklistItemGraphQLServiceTest {

  @Mock private ChecklistItemService itemService;
  @Mock private ChecklistCategoryService categoryService;
  @Mock private ChecklistDetailService detailService;
  @Mock private ChecklistItemMapper itemMapper;
  @Mock private ChecklistCategoryMapper categoryMapper;
  @Mock private ChecklistDetailMapper detailMapper;

  @InjectMocks private ChecklistItemGraphQLService service;

  @Test
  void items_delegatesToServiceAndMapper() {
    PaginationInput pagination = PaginationInput.newBuilder().page(0).size(10).build();
    ChecklistItemEntity entity = new ChecklistItemEntity();
    entity.setId(UUID.randomUUID());
    Page<ChecklistItemEntity> page = new PageImpl<>(List.of(entity));
    when(itemService.findAllPaged(any(Pageable.class))).thenReturn(page);
    ChecklistItem mapped = ChecklistItem.newBuilder().id(entity.getId().toString()).name("x").build();
    when(itemMapper.map(entity)).thenReturn(mapped);

    PagedChecklistItems result = service.items(pagination);

    assertThat(result.getItems()).containsExactly(mapped);
    verify(itemService).findAllPaged(any(Pageable.class));
    verify(itemMapper).map(entity);
  }

  @Test
  void item_delegatesToLocateAndMap() {
    UUID id = UUID.randomUUID();
    ChecklistItemEntity entity = new ChecklistItemEntity();
    entity.setId(id);
    when(itemService.locate(id)).thenReturn(Optional.of(entity));
    ChecklistItem mapped = ChecklistItem.newBuilder().id(id.toString()).name("x").build();
    when(itemMapper.map(entity)).thenReturn(mapped);

    ChecklistItem result = service.item(id.toString());

    assertThat(result).isEqualTo(mapped);
    verify(itemService).locate(id);
  }

  @Test
  void createItem_savesAndReturnsSuccess() {
    ChecklistItemEntity saved = new ChecklistItemEntity();
    saved.setId(UUID.randomUUID());
    when(itemService.save(any(ChecklistItemEntity.class))).thenReturn(saved);

    QueryResult result = service.createItem("Buy milk", "desc", null, "icon");

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(saved.getId().toString());
    verify(itemService).save(any(ChecklistItemEntity.class));
  }

  @Test
  void retireItem_delegatesToService() {
    UUID id = UUID.randomUUID();
    ChecklistItemEntity retired = new ChecklistItemEntity();
    retired.setId(id);
    when(itemService.retire(id)).thenReturn(retired);

    QueryResult result = service.retireItem(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    verify(itemService).retire(id);
  }

  @Test
  void listCategories_delegatesToService() {
    ChecklistCategoryEntity entity = new ChecklistCategoryEntity();
    entity.setId(UUID.randomUUID());
    when(categoryService.findAll()).thenReturn(List.of(entity));
    ChecklistCategory mapped = ChecklistCategory.newBuilder().id(entity.getId().toString()).name("cat").build();
    when(categoryMapper.map(entity)).thenReturn(mapped);

    List<ChecklistCategory> result = service.listCategories();

    assertThat(result).containsExactly(mapped);
    verify(categoryService).findAll();
  }
}

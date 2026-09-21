package com.sun.echo.graphql.services;

import com.sun.base.error.MutationException;
import com.sun.echo.codegen.types.ChecklistCategory;
import com.sun.echo.codegen.types.ChecklistCategoryInput;
import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistItem;
import com.sun.echo.codegen.types.ChecklistItemInput;
import com.sun.echo.codegen.types.CreateCategoryResponse;
import com.sun.echo.codegen.types.CreateItemResponse;
import com.sun.echo.codegen.types.PagedChecklistItems;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.RetireItemResponse;
import com.sun.echo.codegen.types.SaveCategoryResponse;
import com.sun.echo.codegen.types.SaveItemResponse;
import com.sun.echo.graphql.mappers.ChecklistCategoryMapper;
import com.sun.echo.graphql.mappers.ChecklistDetailMapper;
import com.sun.echo.graphql.mappers.ChecklistItemMapper;
import com.sun.echo.model.ChecklistCategoryEntity;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.service.ChecklistCategoryService;
import com.sun.echo.service.ChecklistDetailService;
import com.sun.echo.service.ChecklistItemService;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for checklist items and categories.
 */
@Service
public class ChecklistItemGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ChecklistItemGraphQLService.class);

  private final ChecklistItemService itemService;
  private final ChecklistCategoryService categoryService;
  private final ChecklistDetailService detailService;

  private final ChecklistItemMapper itemMapper;
  private final ChecklistCategoryMapper categoryMapper;
  private final ChecklistDetailMapper detailMapper;

  public ChecklistItemGraphQLService(ChecklistItemService itemService,
      ChecklistCategoryService categoryService, ChecklistDetailService detailService,
      ChecklistItemMapper itemMapper, ChecklistCategoryMapper categoryMapper,
      ChecklistDetailMapper detailMapper) {
    this.itemService = itemService;
    this.categoryService = categoryService;
    this.detailService = detailService;
    this.itemMapper = itemMapper;
    this.categoryMapper = categoryMapper;
    this.detailMapper = detailMapper;
  }

  /**
   * Lists checklist items as a page, sorted by name by default.
   *
   * @param pagination the pagination and sort input
   * @return a page of checklist items
   */
  @Transactional(readOnly = true)
  public PagedChecklistItems items(PaginationInput pagination) {
    Page<ChecklistItemEntity> result = itemService.findAllPaged(
        EchoGraphQLSupport.toPageable(pagination, "name", Sort.Direction.ASC));
    List<ChecklistItem> items = result.getContent().stream().map(itemMapper::map).collect(Collectors.toList());
    return PagedChecklistItems.newBuilder()
        .items(items)
        .pageInfo(EchoGraphQLSupport.pageInfo(result))
        .build();
  }

  /**
   * Locates a single checklist item by id.
   *
   * @param id the item id
   * @return the GraphQL ChecklistItem, or null if not found
   */
  @Transactional(readOnly = true)
  public ChecklistItem item(String id) {
    return itemService.locate(UUID.fromString(id)).map(itemMapper::map).orElse(null);
  }

  /**
   * Locates the detail sidecar for an item.
   *
   * @param id the item id
   * @return the GraphQL ChecklistDetail, or null if none exists
   */
  @Transactional(readOnly = true)
  public ChecklistDetail itemDetails(String id) {
    return detailService.findItemDetail(UUID.fromString(id)).map(detailMapper::map).orElse(null);
  }

  /**
   * Lists every checklist category.
   *
   * @return the GraphQL ChecklistCategories
   */
  @Transactional(readOnly = true)
  public List<ChecklistCategory> listCategories() {
    return categoryService.findAll().stream().map(categoryMapper::map).collect(Collectors.toList());
  }

  /**
   * Creates a new checklist item.
   *
   * @param name the item name
   * @param description an optional description
   * @param categoryId an optional category id
   * @param icon an optional icon name
   * @return the created checklist item
   */
  @Transactional
  public CreateItemResponse createItem(String name, String description, String categoryId, String icon) {
    return mutate("createItem", () -> {
      ChecklistItemEntity entity = new ChecklistItemEntity();
      entity.setName(name);
      entity.setDescription(description);
      entity.setIcon(icon);
      if (categoryId != null) {
        entity.setCategoryId(UUID.fromString(categoryId));
      }
      ChecklistItemEntity saved = itemService.save(entity);
      return CreateItemResponse.newBuilder()
          .message("Checklist item created successfully")
          .item(itemMapper.map(saved))
          .build();
    });
  }

  /**
   * Creates or updates a checklist item from input.
   *
   * @param input the item input
   * @return the saved checklist item
   */
  @Transactional
  public SaveItemResponse saveItem(ChecklistItemInput input) {
    return mutate("saveItem", () -> {
      ChecklistItemEntity entity = resolveItem(input.getId());
      itemMapper.map(input, entity);
      ChecklistItemEntity saved = itemService.save(entity);
      return SaveItemResponse.newBuilder()
          .message("Checklist item saved successfully")
          .item(itemMapper.map(saved))
          .build();
    });
  }

  /**
   * Soft-retires a checklist item.
   *
   * @param id the item id
   * @return the retired checklist item
   */
  @Transactional
  public RetireItemResponse retireItem(String id) {
    return mutate("retireItem", () -> {
      ChecklistItemEntity retired = itemService.retire(UUID.fromString(id));
      return RetireItemResponse.newBuilder()
          .message("Checklist item retired successfully")
          .item(itemMapper.map(retired))
          .build();
    });
  }

  /**
   * Creates a new checklist category.
   *
   * @param name the category name
   * @param description an optional description
   * @return the created checklist category
   */
  @Transactional
  public CreateCategoryResponse createCategory(String name, String description) {
    return mutate("createCategory", () -> {
      ChecklistCategoryEntity entity = new ChecklistCategoryEntity();
      entity.setName(name);
      entity.setDescription(description);
      ChecklistCategoryEntity saved = categoryService.save(entity);
      return CreateCategoryResponse.newBuilder()
          .message("Checklist category created successfully")
          .category(categoryMapper.map(saved))
          .build();
    });
  }

  /**
   * Creates or updates a checklist category from input.
   *
   * @param input the category input
   * @return the saved checklist category
   */
  @Transactional
  public SaveCategoryResponse saveCategory(ChecklistCategoryInput input) {
    return mutate("saveCategory", () -> {
      ChecklistCategoryEntity entity = resolveCategory(input.getId());
      categoryMapper.map(input, entity);
      ChecklistCategoryEntity saved = categoryService.save(entity);
      return SaveCategoryResponse.newBuilder()
          .message("Checklist category saved successfully")
          .category(categoryMapper.map(saved))
          .build();
    });
  }

  /**
   * Resolves an existing item (update) or returns a fresh one (create).
   *
   * @param id an optional existing item id
   * @return the item entity
   */
  private ChecklistItemEntity resolveItem(String id) {
    if (id == null) {
      return new ChecklistItemEntity();
    }
    return itemService.locate(UUID.fromString(id))
        .orElseThrow(() -> new IllegalArgumentException("Checklist item not found: " + id));
  }

  /**
   * Resolves an existing category (update) or returns a fresh one (create).
   *
   * @param id an optional existing category id
   * @return the category entity
   */
  private ChecklistCategoryEntity resolveCategory(String id) {
    if (id == null) {
      return new ChecklistCategoryEntity();
    }
    return categoryService.locate(UUID.fromString(id))
        .orElseThrow(() -> new IllegalArgumentException("Checklist category not found: " + id));
  }

  /**
   * Runs a mutation, logging the operation and raising a MutationException on failure.
   *
   * @param op the operation name, for logging and messages
   * @param action the mutation, returning its response
   * @return the mutation response
   */
  private <T> T mutate(String op, Supplier<T> action) {
    try {
      T response = action.get();
      logger.info("{} succeeded", op);
      return response;
    } catch (Exception e) {
      logger.error("{} failed", op, e);
      throw new MutationException(op + " failed: " + e.getMessage(), e);
    }
  }
}

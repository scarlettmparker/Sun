package com.sun.echo.graphql.services;

import com.sun.base.util.PageRequests;
import com.sun.echo.codegen.types.ChecklistCategory;
import com.sun.echo.codegen.types.ChecklistCategoryInput;
import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistEntry;
import com.sun.echo.codegen.types.ChecklistEntryInput;
import com.sun.echo.codegen.types.ChecklistEntryItem;
import com.sun.echo.codegen.types.ChecklistItem;
import com.sun.echo.codegen.types.ChecklistItemInput;
import com.sun.echo.codegen.types.PagedChecklistEntryItems;
import com.sun.echo.codegen.types.PagedChecklistItems;
import com.sun.echo.codegen.types.PagedChecklistTemplateItems;
import com.sun.echo.codegen.types.PageInfo;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.ChecklistTemplate;
import com.sun.echo.codegen.types.ChecklistTemplateInput;
import com.sun.echo.codegen.types.ChecklistTemplateItem;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.codegen.types.RemoteObjectReference;
import com.sun.echo.codegen.types.RemoteObjectType;
import com.sun.echo.codegen.types.StandardError;
import com.sun.echo.graphql.mappers.ChecklistCategoryMapper;
import com.sun.echo.graphql.mappers.ChecklistDetailMapper;
import com.sun.echo.graphql.mappers.ChecklistEntryItemMapper;
import com.sun.echo.graphql.mappers.ChecklistEntryMapper;
import com.sun.echo.graphql.mappers.ChecklistItemMapper;
import com.sun.echo.graphql.mappers.ChecklistTemplateItemMapper;
import com.sun.echo.graphql.mappers.ChecklistTemplateMapper;
import com.sun.echo.model.ChecklistCategoryEntity;
import com.sun.echo.model.ChecklistEntryEntity;
import com.sun.echo.model.ChecklistEntryItemEntity;
import com.sun.echo.model.ChecklistItemEntity;
import com.sun.echo.model.ChecklistTemplateEntity;
import com.sun.echo.model.ChecklistTemplateItemEntity;
import com.sun.echo.model.enums.ItemStatus;
import com.sun.echo.service.ChecklistCategoryService;
import com.sun.echo.service.ChecklistDetailService;
import com.sun.echo.service.ChecklistEntryItemService;
import com.sun.echo.service.ChecklistEntryService;
import com.sun.echo.service.ChecklistItemService;
import com.sun.echo.service.ChecklistTemplateItemService;
import com.sun.echo.service.ChecklistTemplateService;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for checklists.
 */
@Service
public class ChecklistGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ChecklistGraphQLService.class);

  private final ChecklistItemService itemService;
  private final ChecklistCategoryService categoryService;
  private final ChecklistEntryService entryService;
  private final ChecklistTemplateService templateService;
  private final ChecklistEntryItemService entryItemService;
  private final ChecklistTemplateItemService templateItemService;
  private final ChecklistDetailService detailService;

  private final ChecklistItemMapper itemMapper;
  private final ChecklistCategoryMapper categoryMapper;
  private final ChecklistEntryMapper entryMapper;
  private final ChecklistTemplateMapper templateMapper;
  private final ChecklistEntryItemMapper entryItemMapper;
  private final ChecklistTemplateItemMapper templateItemMapper;
  private final ChecklistDetailMapper detailMapper;

  public ChecklistGraphQLService(ChecklistItemService itemService,
      ChecklistCategoryService categoryService, ChecklistEntryService entryService,
      ChecklistTemplateService templateService, ChecklistEntryItemService entryItemService,
      ChecklistTemplateItemService templateItemService, ChecklistDetailService detailService,
      ChecklistItemMapper itemMapper, ChecklistCategoryMapper categoryMapper,
      ChecklistEntryMapper entryMapper, ChecklistTemplateMapper templateMapper,
      ChecklistEntryItemMapper entryItemMapper, ChecklistTemplateItemMapper templateItemMapper,
      ChecklistDetailMapper detailMapper) {
    this.itemService = itemService;
    this.categoryService = categoryService;
    this.entryService = entryService;
    this.templateService = templateService;
    this.entryItemService = entryItemService;
    this.templateItemService = templateItemService;
    this.detailService = detailService;
    this.itemMapper = itemMapper;
    this.categoryMapper = categoryMapper;
    this.entryMapper = entryMapper;
    this.templateMapper = templateMapper;
    this.entryItemMapper = entryItemMapper;
    this.templateItemMapper = templateItemMapper;
    this.detailMapper = detailMapper;
  }

  /**
   * Lists checklist items as a page, sorted by name by default.
   *
   * @param pagination the pagination and sort input
   * @return a page of checklist items
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public PagedChecklistItems items(PaginationInput pagination) {
    Page<ChecklistItemEntity> result = itemService.findAllPaged(toPageable(pagination, "name", Sort.Direction.ASC));
    List<ChecklistItem> items = result.getContent().stream().map(itemMapper::map).collect(Collectors.toList());
    return PagedChecklistItems.newBuilder()
        .items(items)
        .pageInfo(pageInfo(result))
        .build();
  }

  /**
   * Locates a single checklist item by id.
   *
   * @param id the item id
   * @return the GraphQL ChecklistItem, or null if not found
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public ChecklistItem item(String id) {
    return itemService.locate(UUID.fromString(id)).map(itemMapper::map).orElse(null);
  }

  /**
   * Locates a single checklist entry by id.
   *
   * @param id the entry id
   * @return the GraphQL ChecklistEntry, or null if not found
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public ChecklistEntry entry(String id) {
    return entryService.locate(UUID.fromString(id)).map(entryMapper::map).orElse(null);
  }

  /**
   * Locates a template by id without its detail sidecar.
   *
   * @param id the template id
   * @return the GraphQL ChecklistTemplate, or null if not found
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public ChecklistTemplate template(String id) {
    return templateService.locate(UUID.fromString(id)).map(templateMapper::map).orElse(null);
  }

  /**
   * Locates the detail sidecar for a template.
   *
   * @param id the template id
   * @return the GraphQL ChecklistDetail, or null if none exists
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public ChecklistDetail templateDetails(String id) {
    return detailService.findTemplateDetail(UUID.fromString(id)).map(detailMapper::map).orElse(null);
  }

  /**
   * Locates the detail sidecar for an entry.
   *
   * @param id the entry id
   * @return the GraphQL ChecklistDetail, or null if none exists
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public ChecklistDetail entryDetails(String id) {
    return detailService.findEntryDetail(UUID.fromString(id)).map(detailMapper::map).orElse(null);
  }

  /**
   * Locates the detail sidecar for an item.
   *
   * @param id the item id
   * @return the GraphQL ChecklistDetail, or null if none exists
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public ChecklistDetail itemDetails(String id) {
    return detailService.findItemDetail(UUID.fromString(id)).map(detailMapper::map).orElse(null);
  }

  /**
   * Lists the items belonging to a template as a page, sorted by position by default.
   *
   * @param templateId the template id
   * @param pagination the pagination and sort input
   * @return a page of template items
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public PagedChecklistTemplateItems templateItems(String templateId, PaginationInput pagination) {
    Page<ChecklistTemplateItemEntity> result = templateItemService
        .listForTemplatePaged(UUID.fromString(templateId), toPageable(pagination, "position", Sort.Direction.ASC));
    List<ChecklistTemplateItem> items = templateItemMapper.map(result.getContent());
    return PagedChecklistTemplateItems.newBuilder()
        .items(items)
        .pageInfo(pageInfo(result))
        .build();
  }

  /**
   * Lists the items belonging to an entry as a page, sorted by position by default.
   *
   * @param entryId the entry id
   * @param pagination the pagination and sort input
   * @return a page of entry items
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public PagedChecklistEntryItems entryItems(String entryId, PaginationInput pagination) {
    Page<ChecklistEntryItemEntity> result = entryItemService
        .listForEntryPaged(UUID.fromString(entryId), toPageable(pagination, "position", Sort.Direction.ASC));
    List<ChecklistEntryItem> items = entryItemMapper.map(result.getContent());
    return PagedChecklistEntryItems.newBuilder()
        .items(items)
        .pageInfo(pageInfo(result))
        .build();
  }

  /**
   * Lists every checklist entry.
   *
   * @return the GraphQL ChecklistEntries
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public List<ChecklistEntry> listEntries() {
    return entryService.findAll().stream().map(entryMapper::map).collect(Collectors.toList());
  }

  /**
   * Lists every checklist template.
   *
   * @return the GraphQL ChecklistTemplates
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public List<ChecklistTemplate> listTemplates() {
    return templateService.findAll().stream().map(templateMapper::map).collect(Collectors.toList());
  }

  /**
   * Lists every checklist category.
   *
   * @return the GraphQL ChecklistCategories
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public List<ChecklistCategory> listCategories() {
    return categoryService.findAll().stream().map(categoryMapper::map).collect(Collectors.toList());
  }

  /**
   * Finds every checklist detail that references any of the given remote-object
   * ids, tagged with the owning entity type.
   *
   * @param ids the foreign object ids to resolve
   * @return the GraphQL RemoteObjectReferences
   */
  @Transactional(value = "echoTransactionManager", readOnly = true)
  public List<RemoteObjectReference> locateRemoteObjects(List<String> ids) {
    return detailService.locateRemoteObjects(ids).stream()
        .map(ref -> RemoteObjectReference.newBuilder()
            .id(ref.id().toString())
            .ownerType(RemoteObjectType.valueOf(ref.ownerType()))
            .ownerId(ref.ownerId().toString())
            .description(ref.description())
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Creates a new checklist item.
   *
   * @param name the item name
   * @param description an optional description
   * @param categoryId an optional category id
   * @param icon an optional icon name
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult createItem(String name, String description, String categoryId, String icon) {
    return mutate("createItem", () -> {
      ChecklistItemEntity entity = new ChecklistItemEntity();
      entity.setName(name);
      entity.setDescription(description);
      entity.setIcon(icon);
      if (categoryId != null) {
        entity.setCategoryId(UUID.fromString(categoryId));
      }
      return itemService.save(entity).getId();
    });
  }

  /**
   * Creates or updates a checklist item from input.
   *
   * @param input the item input
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult saveItem(ChecklistItemInput input) {
    return mutate("saveItem", () -> {
      ChecklistItemEntity entity = resolveItem(input.getId());
      itemMapper.map(input, entity);
      return itemService.save(entity).getId();
    });
  }

  /**
   * Soft-retires a checklist item.
   *
   * @param id the item id
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult retireItem(String id) {
    return mutate("retireItem", () -> itemService.retire(UUID.fromString(id)).getId());
  }

  /**
   * Creates a new checklist category.
   *
   * @param name the category name
   * @param description an optional description
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult createCategory(String name, String description) {
    return mutate("createCategory", () -> {
      ChecklistCategoryEntity entity = new ChecklistCategoryEntity();
      entity.setName(name);
      entity.setDescription(description);
      return categoryService.save(entity).getId();
    });
  }

  /**
   * Creates or updates a checklist category from input.
   *
   * @param input the category input
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult saveCategory(ChecklistCategoryInput input) {
    return mutate("saveCategory", () -> {
      ChecklistCategoryEntity entity = resolveCategory(input.getId());
      categoryMapper.map(input, entity);
      return categoryService.save(entity).getId();
    });
  }

  /**
   * Creates an empty checklist entry.
   *
   * @param name an optional name
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult createChecklist(String name) {
    return mutate("createChecklist", () -> {
      ChecklistEntryEntity entity = new ChecklistEntryEntity();
      entity.setName(name);
      return entryService.save(entity).getId();
    });
  }

  /**
   * Creates a checklist entry seeded from a template's items.
   *
   * @param templateId the template id
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult createChecklistFromTemplate(String templateId) {
    return mutate("createChecklistFromTemplate",
        () -> entryService.createFromTemplate(UUID.fromString(templateId)).getId());
  }

  /**
   * Creates a checklist entry composed from multiple templates.
   *
   * @param templateIds the template ids to compose
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult createChecklistFromTemplates(List<String> templateIds) {
    return mutate("createChecklistFromTemplates",
        () -> entryService.createFromTemplates(
            templateIds.stream().map(UUID::fromString).collect(Collectors.toList())).getId());
  }

  /**
   * Creates or updates a checklist entry from input.
   *
   * @param input the entry input
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult saveChecklist(ChecklistEntryInput input) {
    return mutate("saveChecklist", () -> {
      ChecklistEntryEntity entity = resolveEntry(input.getId());
      entryMapper.map(input, entity);
      return entryService.save(entity).getId();
    });
  }

  /**
   * Stamps a checklist entry's completion timestamp.
   *
   * @param id the entry id
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult completeChecklist(String id) {
    return mutate("completeChecklist", () -> entryService.completeChecklist(UUID.fromString(id)).getId());
  }

  /**
   * Archives a checklist entry (entry items are preserved).
   *
   * @param id the entry id
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult archiveChecklist(String id) {
    return mutate("archiveChecklist", () -> entryService.archive(UUID.fromString(id)).getId());
  }

  /**
   * Creates a template, optionally seeded with pre-selected items.
   *
   * @param name the template name
   * @param description an optional description
   * @param itemIds optional item ids to seed
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult createTemplate(String name, String description, List<String> itemIds) {
    return mutate("createTemplate", () -> {
      ChecklistTemplateEntity entity = new ChecklistTemplateEntity();
      entity.setName(name);
      entity.setDescription(description);
      UUID templateId = templateService.save(entity).getId();
      if (itemIds != null && !itemIds.isEmpty()) {
        templateService.addItems(templateId, itemIds.stream().map(UUID::fromString).collect(Collectors.toList()));
      }
      return templateId;
    });
  }

  /**
   * Creates or updates a checklist template from input.
   *
   * @param input the template input
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult saveTemplate(ChecklistTemplateInput input) {
    return mutate("saveTemplate", () -> {
      ChecklistTemplateEntity entity = resolveTemplate(input.getId());
      templateMapper.map(input, entity);
      return templateService.save(entity).getId();
    });
  }

  /**
   * Archives a checklist template.
   *
   * @param id the template id
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult archiveTemplate(String id) {
    return mutate("archiveTemplate", () -> templateService.archive(UUID.fromString(id)).getId());
  }

  /**
   * Adds an item to an entry, auto-positioning when position is null.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param position an optional explicit position
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult addItem(String entryId, String itemId, Integer position) {
    return mutate("addItem",
        () -> entryItemService.addItem(UUID.fromString(entryId), UUID.fromString(itemId), position).getId());
  }

  /**
   * Removes an item from an entry.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult removeItem(String entryId, String itemId) {
    UUID entryUuid = UUID.fromString(entryId);
    return mutate("removeItem", () -> {
      entryItemService.removeItem(entryUuid, UUID.fromString(itemId));
      return entryUuid;
    });
  }

  /**
   * Sets the runtime status of an item within an entry.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param status the new status (NOT_STARTED/COMPLETE/FAILED/NOT_NEEDED)
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult setItemStatus(String entryId, String itemId, ItemStatus status) {
    return mutate("setItemStatus", () -> entryItemService
        .setStatus(UUID.fromString(entryId), UUID.fromString(itemId), status).getId());
  }

  /**
   * Adds an item to a template, auto-positioning when position is null.
   *
   * @param templateId the template id
   * @param itemId the item id
   * @param position an optional explicit position
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult addTemplateItem(String templateId, String itemId, Integer position) {
    return mutate("addTemplateItem", () -> templateItemService
        .addTemplateItem(UUID.fromString(templateId), UUID.fromString(itemId), position).getId());
  }

  /**
   * Removes an item from a template.
   *
   * @param templateId the template id
   * @param itemId the item id
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult removeTemplateItem(String templateId, String itemId) {
    UUID templateUuid = UUID.fromString(templateId);
    return mutate("removeTemplateItem", () -> {
      templateItemService.removeTemplateItem(templateUuid, UUID.fromString(itemId));
      return templateUuid;
    });
  }

  /**
   * Attaches a foreign object to an owner's detail.
   *
   * @param source the owning entity id
   * @param target the foreign object id to attach
   * @param ownerType optional owner type hint
   * @return a QueryResult
   */
  @Transactional("echoTransactionManager")
  public QueryResult attachObject(String source, String target, RemoteObjectType ownerType) {
    return mutate("attachObject", () -> detailService
        .attach(UUID.fromString(source), target, ownerType != null ? ownerType.name() : null));
  }

  /**
   * Converts the pagination input into a pageable, applying the given defaults.
   *
   * @param pagination the pagination and sort input
   * @param defaultSortBy the property to sort by when none is given
   * @param defaultDir the direction when none is given
   * @return the pageable
   */
  private Pageable toPageable(PaginationInput pagination, String defaultSortBy, Sort.Direction defaultDir) {
    if (pagination == null) {
      return PageRequests.of(null, null, null, null, defaultSortBy, defaultDir);
    }
    return PageRequests.of(pagination.getPage(), pagination.getSize(), pagination.getSortBy(),
        pagination.getSortDir() != null ? pagination.getSortDir().name() : null,
        defaultSortBy, defaultDir);
  }

  /**
   * Builds page metadata from a Spring data page.
   *
   * @param result the data page
   * @return the GraphQL PageInfo
   */
  private PageInfo pageInfo(Page<?> result) {
    return PageInfo.newBuilder()
        .page(result.getNumber())
        .size(result.getSize())
        .totalPages(result.getTotalPages())
        .totalCount((int) result.getTotalElements())
        .hasNextPage(result.hasNext())
        .hasPreviousPage(result.hasPrevious())
        .build();
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
   * Resolves an existing entry (update) or returns a fresh one (create).
   *
   * @param id an optional existing entry id
   * @return the entry entity
   */
  private ChecklistEntryEntity resolveEntry(String id) {
    if (id == null) {
      return new ChecklistEntryEntity();
    }
    return entryService.locate(UUID.fromString(id))
        .orElseThrow(() -> new IllegalArgumentException("Checklist entry not found: " + id));
  }

  /**
   * Resolves an existing template (update) or returns a fresh one (create).
   *
   * @param id an optional existing template id
   * @return the template entity
   */
  private ChecklistTemplateEntity resolveTemplate(String id) {
    if (id == null) {
      return new ChecklistTemplateEntity();
    }
    return templateService.locate(UUID.fromString(id))
        .orElseThrow(() -> new IllegalArgumentException("Checklist template not found: " + id));
  }

  /**
   * Runs a mutation, returning QuerySuccess with the affected id or StandardError
   * on failure.
   *
   * @param op the operation name (for logging and messages)
   * @param action the mutation, returning the affected entity id
   * @return a QueryResult
   */
  private QueryResult mutate(String op, Supplier<UUID> action) {
    try {
      UUID id = action.get();
      logger.info("{} succeeded for id {}", op, id);
      return QuerySuccess.newBuilder()
          .message(op + " succeeded")
          .id(id != null ? id.toString() : null)
          .build();
    } catch (Exception e) {
      logger.error("{} failed", op, e);
      return StandardError.newBuilder()
          .message(op + " failed: " + e.getMessage())
          .build();
    }
  }
}

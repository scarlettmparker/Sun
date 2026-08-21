package com.sun.echo.graphql.services;

import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistEntry;
import com.sun.echo.codegen.types.ChecklistEntryInput;
import com.sun.echo.codegen.types.ChecklistEntryItem;
import com.sun.echo.codegen.types.PagedChecklistEntryItems;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.codegen.types.StandardError;
import com.sun.echo.graphql.mappers.ChecklistDetailMapper;
import com.sun.echo.graphql.mappers.ChecklistEntryItemMapper;
import com.sun.echo.graphql.mappers.ChecklistEntryMapper;
import com.sun.echo.model.ChecklistEntryEntity;
import com.sun.echo.model.ChecklistEntryItemEntity;
import com.sun.echo.model.enums.ItemStatus;
import com.sun.echo.service.ChecklistDetailService;
import com.sun.echo.service.ChecklistEntryItemService;
import com.sun.echo.service.ChecklistEntryService;
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
 * GraphQL business logic for checklist entries.
 */
@Service
public class ChecklistEntryGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ChecklistEntryGraphQLService.class);

  private final ChecklistEntryService entryService;
  private final ChecklistEntryItemService entryItemService;
  private final ChecklistDetailService detailService;

  private final ChecklistEntryMapper entryMapper;
  private final ChecklistEntryItemMapper entryItemMapper;
  private final ChecklistDetailMapper detailMapper;

  public ChecklistEntryGraphQLService(ChecklistEntryService entryService,
      ChecklistEntryItemService entryItemService, ChecklistDetailService detailService,
      ChecklistEntryMapper entryMapper, ChecklistEntryItemMapper entryItemMapper,
      ChecklistDetailMapper detailMapper) {
    this.entryService = entryService;
    this.entryItemService = entryItemService;
    this.detailService = detailService;
    this.entryMapper = entryMapper;
    this.entryItemMapper = entryItemMapper;
    this.detailMapper = detailMapper;
  }

  /**
   * Locates a single checklist entry by id.
   *
   * @param id the entry id
   * @return the GraphQL ChecklistEntry, or null if not found
   */
  @Transactional(readOnly = true)
  public ChecklistEntry entry(String id) {
    return entryService.locate(UUID.fromString(id)).map(entryMapper::map).orElse(null);
  }

  /**
   * Locates the detail sidecar for an entry.
   *
   * @param id the entry id
   * @return the GraphQL ChecklistDetail, or null if none exists
   */
  @Transactional(readOnly = true)
  public ChecklistDetail entryDetails(String id) {
    return detailService.findEntryDetail(UUID.fromString(id)).map(detailMapper::map).orElse(null);
  }

  /**
   * Lists every checklist entry.
   *
   * @return the GraphQL ChecklistEntries
   */
  @Transactional(readOnly = true)
  public List<ChecklistEntry> listEntries() {
    return entryService.findAll().stream().map(entryMapper::map).collect(Collectors.toList());
  }

  /**
   * Lists the items belonging to an entry as a page, sorted by position by default.
   *
   * @param entryId the entry id
   * @param pagination the pagination and sort input
   * @return a page of entry items
   */
  @Transactional(readOnly = true)
  public PagedChecklistEntryItems entryItems(String entryId, PaginationInput pagination) {
    Page<ChecklistEntryItemEntity> result = entryItemService
        .listForEntryPaged(UUID.fromString(entryId),
            EchoGraphQLSupport.toPageable(pagination, "position", Sort.Direction.ASC));
    List<ChecklistEntryItem> items = entryItemMapper.map(result.getContent());
    return PagedChecklistEntryItems.newBuilder()
        .items(items)
        .pageInfo(EchoGraphQLSupport.pageInfo(result))
        .build();
  }

  /**
   * Creates an empty checklist entry.
   *
   * @param name an optional name
   * @return a QueryResult
   */
  @Transactional
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
  @Transactional
  public QueryResult createChecklistFromTemplate(String templateId, String name) {
    return mutate("createChecklistFromTemplate",
        () -> entryService.createFromTemplate(UUID.fromString(templateId), name).getId());
  }

  /**
   * Creates a checklist entry composed from multiple templates.
   *
   * @param templateIds the template ids to compose
   * @param name an optional name for the new entry
   * @return a QueryResult
   */
  @Transactional
  public QueryResult createChecklistFromTemplates(List<String> templateIds, String name) {
    return mutate("createChecklistFromTemplates",
        () -> entryService.createFromTemplates(
            templateIds.stream().map(UUID::fromString).collect(Collectors.toList()), name).getId());
  }

  /**
   * Creates or updates a checklist entry from input.
   *
   * @param input the entry input
   * @return a QueryResult
   */
  @Transactional
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
  @Transactional
  public QueryResult completeChecklist(String id) {
    return mutate("completeChecklist", () -> entryService.completeChecklist(UUID.fromString(id)).getId());
  }

  /**
   * Archives a checklist entry (entry items are preserved).
   *
   * @param id the entry id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult archiveChecklist(String id) {
    return mutate("archiveChecklist", () -> entryService.archive(UUID.fromString(id)).getId());
  }

  /**
   * Permanently deletes a checklist entry and its items.
   *
   * @param id the entry id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult deleteChecklist(String id) {
    UUID entryId = UUID.fromString(id);
    return mutate("deleteChecklist", () -> {
      entryService.delete(entryId);
      return entryId;
    });
  }

  /**
   * Adds an item to an entry, auto-positioning when position is null.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param position an optional explicit position
   * @return a QueryResult
   */
  @Transactional
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
  @Transactional
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
  @Transactional
  public QueryResult setItemStatus(String entryId, String itemId, ItemStatus status) {
    return mutate("setItemStatus", () -> entryItemService
        .setStatus(UUID.fromString(entryId), UUID.fromString(itemId), status).getId());
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
          .id(id == null ? null : id.toString())
          .build();
    } catch (Exception e) {
      logger.error("{} failed", op, e);
      return StandardError.newBuilder()
          .message(op + " failed: " + e.getMessage())
          .build();
    }
  }
}

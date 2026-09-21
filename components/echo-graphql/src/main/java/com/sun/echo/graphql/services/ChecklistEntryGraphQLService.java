package com.sun.echo.graphql.services;

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
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.RemoveItemResponse;
import com.sun.echo.codegen.types.SaveChecklistResponse;
import com.sun.echo.codegen.types.SetItemStatusResponse;
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
   * @return the created checklist entry
   */
  @Transactional
  public CreateChecklistResponse createChecklist(String name) {
    return mutate("createChecklist", () -> {
      ChecklistEntryEntity entity = new ChecklistEntryEntity();
      entity.setName(name);
      ChecklistEntryEntity saved = entryService.save(entity);
      return CreateChecklistResponse.newBuilder()
          .message("Checklist created successfully")
          .entry(entryMapper.map(saved))
          .build();
    });
  }

  /**
   * Creates a checklist entry seeded from a template's items.
   *
   * @param templateId the template id
   * @return the created checklist entry
   */
  @Transactional
  public CreateChecklistFromTemplateResponse createChecklistFromTemplate(String templateId, String name) {
    return mutate("createChecklistFromTemplate", () -> {
      ChecklistEntryEntity created = entryService.createFromTemplate(UUID.fromString(templateId), name);
      return CreateChecklistFromTemplateResponse.newBuilder()
          .message("Checklist created from template successfully")
          .entry(entryMapper.map(created))
          .build();
    });
  }

  /**
   * Creates a checklist entry composed from multiple templates.
   *
   * @param templateIds the template ids to compose
   * @param name an optional name for the new entry
   * @return the created checklist entry
   */
  @Transactional
  public CreateChecklistFromTemplatesResponse createChecklistFromTemplates(List<String> templateIds, String name) {
    return mutate("createChecklistFromTemplates", () -> {
      ChecklistEntryEntity created = entryService.createFromTemplates(
          templateIds.stream().map(UUID::fromString).collect(Collectors.toList()), name);
      return CreateChecklistFromTemplatesResponse.newBuilder()
          .message("Checklist created from templates successfully")
          .entry(entryMapper.map(created))
          .build();
    });
  }

  /**
   * Creates or updates a checklist entry from input.
   *
   * @param input the entry input
   * @return the saved checklist entry
   */
  @Transactional
  public SaveChecklistResponse saveChecklist(ChecklistEntryInput input) {
    return mutate("saveChecklist", () -> {
      ChecklistEntryEntity entity = resolveEntry(input.getId());
      entryMapper.map(input, entity);
      ChecklistEntryEntity saved = entryService.save(entity);
      return SaveChecklistResponse.newBuilder()
          .message("Checklist saved successfully")
          .entry(entryMapper.map(saved))
          .build();
    });
  }

  /**
   * Stamps a checklist entry's completion timestamp.
   *
   * @param id the entry id
   * @return the completed checklist entry
   */
  @Transactional
  public CompleteChecklistResponse completeChecklist(String id) {
    return mutate("completeChecklist", () -> {
      ChecklistEntryEntity completed = entryService.completeChecklist(UUID.fromString(id));
      return CompleteChecklistResponse.newBuilder()
          .message("Checklist completed successfully")
          .entry(entryMapper.map(completed))
          .build();
    });
  }

  /**
   * Archives a checklist entry (entry items are preserved).
   *
   * @param id the entry id
   * @return the archived checklist entry
   */
  @Transactional
  public ArchiveChecklistResponse archiveChecklist(String id) {
    return mutate("archiveChecklist", () -> {
      ChecklistEntryEntity archived = entryService.archive(UUID.fromString(id));
      return ArchiveChecklistResponse.newBuilder()
          .message("Checklist archived successfully")
          .entry(entryMapper.map(archived))
          .build();
    });
  }

  /**
   * Permanently deletes a checklist entry and its items.
   *
   * @param id the entry id
   * @return the deleted entry id
   */
  @Transactional
  public DeleteChecklistResponse deleteChecklist(String id) {
    UUID entryId = UUID.fromString(id);
    return mutate("deleteChecklist", () -> {
      entryService.delete(entryId);
      return DeleteChecklistResponse.newBuilder()
          .message("Checklist deleted successfully")
          .id(entryId.toString())
          .build();
    });
  }

  /**
   * Adds an item to an entry, auto-positioning when position is null.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param position an optional explicit position
   * @return the updated checklist entry
   */
  @Transactional
  public AddItemResponse addItem(String entryId, String itemId, Integer position) {
    return mutate("addItem", () -> {
      UUID entryUuid = UUID.fromString(entryId);
      entryItemService.addItem(entryUuid, UUID.fromString(itemId), position);
      return AddItemResponse.newBuilder()
          .message("Checklist item added successfully")
          .entry(entryMapper.map(requireEntry(entryUuid)))
          .build();
    });
  }

  /**
   * Removes an item from an entry.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @return the updated checklist entry
   */
  @Transactional
  public RemoveItemResponse removeItem(String entryId, String itemId) {
    UUID entryUuid = UUID.fromString(entryId);
    return mutate("removeItem", () -> {
      entryItemService.removeItem(entryUuid, UUID.fromString(itemId));
      return RemoveItemResponse.newBuilder()
          .message("Checklist item removed successfully")
          .entry(entryMapper.map(requireEntry(entryUuid)))
          .build();
    });
  }

  /**
   * Sets the runtime status of an item within an entry.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param status the new status (NOT_STARTED/COMPLETE/FAILED/NOT_NEEDED)
   * @return the updated checklist entry
   */
  @Transactional
  public SetItemStatusResponse setItemStatus(String entryId, String itemId, ItemStatus status) {
    return mutate("setItemStatus", () -> {
      UUID entryUuid = UUID.fromString(entryId);
      entryItemService.setStatus(entryUuid, UUID.fromString(itemId), status);
      return SetItemStatusResponse.newBuilder()
          .message("Checklist item status updated successfully")
          .entry(entryMapper.map(requireEntry(entryUuid)))
          .build();
    });
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
    return requireEntry(UUID.fromString(id));
  }

  /**
   * Loads an entry that must exist.
   *
   * @param id the entry id
   * @return the entry entity
   */
  private ChecklistEntryEntity requireEntry(UUID id) {
    return entryService.locate(id)
        .orElseThrow(() -> new IllegalArgumentException("Checklist entry not found: " + id));
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

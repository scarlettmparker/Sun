package com.sun.echo.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
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
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for checklist entry queries and mutations.
 */
@DgsComponent
public class ChecklistEntryDataFetcher {

  private final ChecklistEntryGraphQLService checklistEntryGraphQLService;

  public ChecklistEntryDataFetcher(ChecklistEntryGraphQLService checklistEntryGraphQLService) {
    this.checklistEntryGraphQLService = checklistEntryGraphQLService;
  }

  /**
   * Locates a checklist entry by id.
   *
   * @param id the entry id
   * @return the ChecklistEntry object
   */
  @DgsData(parentType = "ChecklistQueries", field = "entry")
  @PreAuthorize("@permissions.has('graphql.echo.entry')")
  public ChecklistEntry entry(String id) {
    return checklistEntryGraphQLService.entry(id);
  }

  /**
   * Locates the detail for an entry.
   *
   * @param id the entry id
   * @return the ChecklistDetail object
   */
  @DgsData(parentType = "ChecklistQueries", field = "entryDetails")
  @PreAuthorize("@permissions.has('graphql.echo.entryDetails')")
  public ChecklistDetail entryDetails(String id) {
    return checklistEntryGraphQLService.entryDetails(id);
  }

  /**
   * Lists the items belonging to an entry as a page.
   *
   * @param entryId the entry id
   * @param pagination the pagination and sort input
   * @return a page of ChecklistEntryItem objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "entryItems")
  @PreAuthorize("@permissions.has('graphql.echo.entryItems')")
  public PagedChecklistEntryItems entryItems(String entryId, PaginationInput pagination) {
    return checklistEntryGraphQLService.entryItems(entryId, pagination);
  }

  /**
   * Lists all checklist entries.
   *
   * @return a list of ChecklistEntry objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "listEntries")
  @PreAuthorize("@permissions.has('graphql.echo.listEntries')")
  public List<ChecklistEntry> listEntries() {
    return checklistEntryGraphQLService.listEntries();
  }

  /**
   * Creates an empty checklist entry.
   *
   * @param name an optional name
   * @return the created checklist entry
   */
  @DgsData(parentType = "ChecklistMutations", field = "createChecklist")
  @PreAuthorize("@permissions.has('graphql.echo.createChecklist')")
  public CreateChecklistResponse createChecklist(String name) {
    return checklistEntryGraphQLService.createChecklist(name);
  }

  /**
   * Creates a checklist entry seeded from a template's items.
   *
   * @param templateId the template id
   * @return the created checklist entry
   */
  @DgsData(parentType = "ChecklistMutations", field = "createChecklistFromTemplate")
  @PreAuthorize("@permissions.has('graphql.echo.createChecklistFromTemplate')")
  public CreateChecklistFromTemplateResponse createChecklistFromTemplate(String templateId, String name) {
    return checklistEntryGraphQLService.createChecklistFromTemplate(templateId, name);
  }

  /**
   * Creates a checklist entry composed from multiple templates' items.
   *
   * @param templateIds the template ids to compose
   * @param name an optional name for the new entry
   * @return the created checklist entry
   */
  @DgsData(parentType = "ChecklistMutations", field = "createChecklistFromTemplates")
  @PreAuthorize("@permissions.has('graphql.echo.createChecklistFromTemplates')")
  public CreateChecklistFromTemplatesResponse createChecklistFromTemplates(List<String> templateIds, String name) {
    return checklistEntryGraphQLService.createChecklistFromTemplates(templateIds, name);
  }

  /**
   * Creates or updates a checklist entry from input.
   *
   * @param input the entry input
   * @return the saved checklist entry
   */
  @DgsData(parentType = "ChecklistMutations", field = "saveChecklist")
  @PreAuthorize("@permissions.has('graphql.echo.saveChecklist')")
  public SaveChecklistResponse saveChecklist(ChecklistEntryInput input) {
    return checklistEntryGraphQLService.saveChecklist(input);
  }

  /**
   * Stamps a checklist entry's completion timestamp.
   *
   * @param id the entry id
   * @return the completed checklist entry
   */
  @DgsData(parentType = "ChecklistMutations", field = "completeChecklist")
  @PreAuthorize("@permissions.has('graphql.echo.completeChecklist')")
  public CompleteChecklistResponse completeChecklist(String id) {
    return checklistEntryGraphQLService.completeChecklist(id);
  }

  /**
   * Archives a checklist entry.
   *
   * @param id the entry id
   * @return the archived checklist entry
   */
  @DgsData(parentType = "ChecklistMutations", field = "archiveChecklist")
  @PreAuthorize("@permissions.has('graphql.echo.archiveChecklist')")
  public ArchiveChecklistResponse archiveChecklist(String id) {
    return checklistEntryGraphQLService.archiveChecklist(id);
  }

  /**
   * Permanently deletes a checklist entry and its items.
   *
   * @param id the entry id
   * @return the deleted entry id
   */
  @DgsData(parentType = "ChecklistMutations", field = "deleteChecklist")
  @PreAuthorize("@permissions.has('graphql.echo.deleteChecklist')")
  public DeleteChecklistResponse deleteChecklist(String id) {
    return checklistEntryGraphQLService.deleteChecklist(id);
  }

  /**
   * Adds an item to an entry, auto-positioning when position is null.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param position an optional explicit position
   * @return the updated checklist entry
   */
  @DgsData(parentType = "ChecklistMutations", field = "addItem")
  @PreAuthorize("@permissions.has('graphql.echo.addItem')")
  public AddItemResponse addItem(String entryId, String itemId, Integer position) {
    return checklistEntryGraphQLService.addItem(entryId, itemId, position);
  }

  /**
   * Removes an item from an entry.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @return the updated checklist entry
   */
  @DgsData(parentType = "ChecklistMutations", field = "removeItem")
  @PreAuthorize("@permissions.has('graphql.echo.removeItem')")
  public RemoveItemResponse removeItem(String entryId, String itemId) {
    return checklistEntryGraphQLService.removeItem(entryId, itemId);
  }

  /**
   * Sets the runtime status of an item within an entry.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param status the new status (NOT_STARTED, COMPLETE, FAILED, or NOT_NEEDED)
   * @return the updated checklist entry
   */
  @DgsData(parentType = "ChecklistMutations", field = "setItemStatus")
  @PreAuthorize("@permissions.has('graphql.echo.setItemStatus')")
  public SetItemStatusResponse setItemStatus(String entryId, String itemId, ItemStatus status) {
    return checklistEntryGraphQLService.setItemStatus(entryId, itemId, status);
  }
}

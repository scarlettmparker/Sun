package com.sun.echo.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.echo.codegen.types.ChecklistCategory;
import com.sun.echo.codegen.types.ChecklistCategoryInput;
import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistEntry;
import com.sun.echo.codegen.types.ChecklistEntryInput;
import com.sun.echo.codegen.types.ChecklistItem;
import com.sun.echo.codegen.types.ChecklistItemInput;
import com.sun.echo.codegen.types.PagedChecklistEntryItems;
import com.sun.echo.codegen.types.PagedChecklistItems;
import com.sun.echo.codegen.types.PagedChecklistTemplateItems;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.ChecklistMutations;
import com.sun.echo.codegen.types.ChecklistQueries;
import com.sun.echo.codegen.types.ChecklistTemplate;
import com.sun.echo.codegen.types.ChecklistTemplateInput;
import com.sun.echo.codegen.types.ChecklistEntryItem;
import com.sun.echo.codegen.types.ChecklistTemplateItem;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.RemoteObjectReference;
import com.sun.echo.codegen.types.RemoteObjectType;
import com.sun.echo.graphql.services.ChecklistGraphQLService;
import com.sun.echo.model.enums.ItemStatus;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Data fetchers for the checklist queries and mutations.
 */
@DgsComponent
public class ChecklistDataFetcher {

  @Autowired
  private ChecklistGraphQLService checklistGraphQLService;

  /**
   * Provides the checklist queries object.
   *
   * @return a new ChecklistQueries instance
   */
  @DgsData(parentType = "Query", field = "checklistQueries")
  public ChecklistQueries getChecklistQueries() {
    return ChecklistQueries.newBuilder().build();
  }

  /**
   * Lists checklist items as a page.
   *
   * @param pagination the pagination and sort input
   * @return a page of ChecklistItem objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "items")
  public PagedChecklistItems items(PaginationInput pagination) {
    return checklistGraphQLService.items(pagination);
  }

  /**
   * Locates a checklist item by id.
   *
   * @param id the item id
   * @return the ChecklistItem object
   */
  @DgsData(parentType = "ChecklistQueries", field = "item")
  public ChecklistItem item(String id) {
    return checklistGraphQLService.item(id);
  }

  /**
   * Locates a checklist entry by id.
   *
   * @param id the entry id
   * @return the ChecklistEntry object
   */
  @DgsData(parentType = "ChecklistQueries", field = "entry")
  public ChecklistEntry entry(String id) {
    return checklistGraphQLService.entry(id);
  }

  /**
   * Locates a checklist template by id, without its detail.
   *
   * @param id the template id
   * @return the ChecklistTemplate object
   */
  @DgsData(parentType = "ChecklistQueries", field = "template")
  public ChecklistTemplate template(String id) {
    return checklistGraphQLService.template(id);
  }

  /**
   * Locates the detail for a template.
   *
   * @param id the template id
   * @return the ChecklistDetail object
   */
  @DgsData(parentType = "ChecklistQueries", field = "templateDetails")
  public ChecklistDetail templateDetails(String id) {
    return checklistGraphQLService.templateDetails(id);
  }

  /**
   * Locates the detail for an entry.
   *
   * @param id the entry id
   * @return the ChecklistDetail object
   */
  @DgsData(parentType = "ChecklistQueries", field = "entryDetails")
  public ChecklistDetail entryDetails(String id) {
    return checklistGraphQLService.entryDetails(id);
  }

  /**
   * Locates the detail for an item.
   *
   * @param id the item id
   * @return the ChecklistDetail object
   */
  @DgsData(parentType = "ChecklistQueries", field = "itemDetails")
  public ChecklistDetail itemDetails(String id) {
    return checklistGraphQLService.itemDetails(id);
  }

  /**
   * Lists the items belonging to a template as a page.
   *
   * @param templateId the template id
   * @param pagination the pagination and sort input
   * @return a page of ChecklistTemplateItem objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "templateItems")
  public PagedChecklistTemplateItems templateItems(String templateId, PaginationInput pagination) {
    return checklistGraphQLService.templateItems(templateId, pagination);
  }

  /**
   * Lists the items belonging to an entry as a page.
   *
   * @param entryId the entry id
   * @param pagination the pagination and sort input
   * @return a page of ChecklistEntryItem objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "entryItems")
  public PagedChecklistEntryItems entryItems(String entryId, PaginationInput pagination) {
    return checklistGraphQLService.entryItems(entryId, pagination);
  }

  /**
   * Lists all checklist entries.
   *
   * @return a list of ChecklistEntry objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "listEntries")
  public List<ChecklistEntry> listEntries() {
    return checklistGraphQLService.listEntries();
  }

  /**
   * Lists all checklist templates.
   *
   * @return a list of ChecklistTemplate objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "listTemplates")
  public List<ChecklistTemplate> listTemplates() {
    return checklistGraphQLService.listTemplates();
  }

  /**
   * Lists all checklist categories.
   *
   * @return a list of ChecklistCategory objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "listCategories")
  public List<ChecklistCategory> listCategories() {
    return checklistGraphQLService.listCategories();
  }

  /**
   * Finds the checklist details that reference any of the given object ids.
   *
   * @param ids the object ids to resolve
   * @return a list of RemoteObjectReference objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "locateRemoteObjects")
  public List<RemoteObjectReference> locateRemoteObjects(List<String> ids) {
    return checklistGraphQLService.locateRemoteObjects(ids);
  }

  /**
   * Provides the checklist mutations object.
   *
   * @return a new ChecklistMutations instance
   */
  @DgsData(parentType = "Mutation", field = "checklistMutations")
  public ChecklistMutations getChecklistMutations() {
    return ChecklistMutations.newBuilder().build();
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
  @DgsData(parentType = "ChecklistMutations", field = "createItem")
  public QueryResult createItem(String name, String description, String categoryId, String icon) {
    return checklistGraphQLService.createItem(name, description, categoryId, icon);
  }

  /**
   * Creates or updates a checklist item from input.
   *
   * @param input the item input
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "saveItem")
  public QueryResult saveItem(ChecklistItemInput input) {
    return checklistGraphQLService.saveItem(input);
  }

  /**
   * Soft-retires a checklist item.
   *
   * @param id the item id
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "retireItem")
  public QueryResult retireItem(String id) {
    return checklistGraphQLService.retireItem(id);
  }

  /**
   * Creates a new checklist category.
   *
   * @param name the category name
   * @param description an optional description
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "createCategory")
  public QueryResult createCategory(String name, String description) {
    return checklistGraphQLService.createCategory(name, description);
  }

  /**
   * Creates or updates a checklist category from input.
   *
   * @param input the category input
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "saveCategory")
  public QueryResult saveCategory(ChecklistCategoryInput input) {
    return checklistGraphQLService.saveCategory(input);
  }

  /**
   * Creates an empty checklist entry.
   *
   * @param name an optional name
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "createChecklist")
  public QueryResult createChecklist(String name) {
    return checklistGraphQLService.createChecklist(name);
  }

  /**
   * Creates a checklist entry seeded from a template's items.
   *
   * @param templateId the template id
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "createChecklistFromTemplate")
  public QueryResult createChecklistFromTemplate(String templateId, String name) {
    return checklistGraphQLService.createChecklistFromTemplate(templateId, name);
  }

  /**
   * Creates a checklist entry composed from multiple templates' items.
   *
   * @param templateIds the template ids to compose
   * @param name an optional name for the new entry
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "createChecklistFromTemplates")
  public QueryResult createChecklistFromTemplates(List<String> templateIds, String name) {
    return checklistGraphQLService.createChecklistFromTemplates(templateIds, name);
  }

  /**
   * Creates or updates a checklist entry from input.
   *
   * @param input the entry input
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "saveChecklist")
  public QueryResult saveChecklist(ChecklistEntryInput input) {
    return checklistGraphQLService.saveChecklist(input);
  }

  /**
   * Stamps a checklist entry's completion timestamp.
   *
   * @param id the entry id
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "completeChecklist")
  public QueryResult completeChecklist(String id) {
    return checklistGraphQLService.completeChecklist(id);
  }

  /**
   * Archives a checklist entry.
   *
   * @param id the entry id
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "archiveChecklist")
  public QueryResult archiveChecklist(String id) {
    return checklistGraphQLService.archiveChecklist(id);
  }

  /**
   * Permanently deletes a checklist entry and its items.
   *
   * @param id the entry id
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "deleteChecklist")
  public QueryResult deleteChecklist(String id) {
    return checklistGraphQLService.deleteChecklist(id);
  }

  /**
   * Creates a template, optionally seeded with pre-selected items.
   *
   * @param name the template name
   * @param description an optional description
   * @param itemIds optional item ids to seed
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "createTemplate")
  public QueryResult createTemplate(String name, String description, List<String> itemIds) {
    return checklistGraphQLService.createTemplate(name, description, itemIds);
  }

  /**
   * Creates or updates a checklist template from input.
   *
   * @param input the template input
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "saveTemplate")
  public QueryResult saveTemplate(ChecklistTemplateInput input) {
    return checklistGraphQLService.saveTemplate(input);
  }

  /**
   * Archives a checklist template.
   *
   * @param id the template id
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "archiveTemplate")
  public QueryResult archiveTemplate(String id) {
    return checklistGraphQLService.archiveTemplate(id);
  }

  /**
   * Adds an item to an entry, auto-positioning when position is null.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param position an optional explicit position
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "addItem")
  public QueryResult addItem(String entryId, String itemId, Integer position) {
    return checklistGraphQLService.addItem(entryId, itemId, position);
  }

  /**
   * Removes an item from an entry.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "removeItem")
  public QueryResult removeItem(String entryId, String itemId) {
    return checklistGraphQLService.removeItem(entryId, itemId);
  }

  /**
   * Sets the runtime status of an item within an entry.
   *
   * @param entryId the entry id
   * @param itemId the item id
   * @param status the new status (NOT_STARTED, COMPLETE, FAILED, or NOT_NEEDED)
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "setItemStatus")
  public QueryResult setItemStatus(String entryId, String itemId, ItemStatus status) {
    return checklistGraphQLService.setItemStatus(entryId, itemId, status);
  }

  /**
   * Adds an item to a template, auto-positioning when position is null.
   *
   * @param templateId the template id
   * @param itemId the item id
   * @param position an optional explicit position
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "addTemplateItem")
  public QueryResult addTemplateItem(String templateId, String itemId, Integer position) {
    return checklistGraphQLService.addTemplateItem(templateId, itemId, position);
  }

  /**
   * Removes an item from a template.
   *
   * @param templateId the template id
   * @param itemId the item id
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "removeTemplateItem")
  public QueryResult removeTemplateItem(String templateId, String itemId) {
    return checklistGraphQLService.removeTemplateItem(templateId, itemId);
  }

  /**
   * Attaches a foreign object to a checklist detail.
   *
   * @param source the owning entity id
   * @param target the foreign object id to attach
   * @param ownerType an optional owner type hint (ENTRY, TEMPLATE, or ITEM)
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "attachObject")
  public QueryResult attachObject(String source, String target, RemoteObjectType ownerType) {
    return checklistGraphQLService.attachObject(source, target, ownerType);
  }

  @DgsData(parentType = "ChecklistMutations", field = "detachObject")
  public QueryResult detachObject(String source, String target, RemoteObjectType ownerType) {
    return checklistGraphQLService.detachObject(source, target, ownerType);
  }
}

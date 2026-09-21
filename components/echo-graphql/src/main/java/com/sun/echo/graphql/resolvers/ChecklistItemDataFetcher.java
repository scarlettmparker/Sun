package com.sun.echo.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.echo.codegen.types.ChecklistCategory;
import com.sun.echo.codegen.types.ChecklistCategoryInput;
import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistItem;
import com.sun.echo.codegen.types.ChecklistItemInput;
import com.sun.echo.codegen.types.PagedChecklistItems;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.ChecklistMutations;
import com.sun.echo.codegen.types.ChecklistQueries;
import com.sun.echo.codegen.types.CreateCategoryResponse;
import com.sun.echo.codegen.types.CreateItemResponse;
import com.sun.echo.codegen.types.RetireItemResponse;
import com.sun.echo.codegen.types.SaveCategoryResponse;
import com.sun.echo.codegen.types.SaveItemResponse;
import com.sun.echo.graphql.services.ChecklistItemGraphQLService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for checklist item and category queries and mutations.
 */
@DgsComponent
public class ChecklistItemDataFetcher {

  private final ChecklistItemGraphQLService checklistItemGraphQLService;

  public ChecklistItemDataFetcher(ChecklistItemGraphQLService checklistItemGraphQLService) {
    this.checklistItemGraphQLService = checklistItemGraphQLService;
  }

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
   * Provides the checklist mutations object.
   *
   * @return a new ChecklistMutations instance
   */
  @DgsData(parentType = "Mutation", field = "checklistMutations")
  public ChecklistMutations getChecklistMutations() {
    return ChecklistMutations.newBuilder().build();
  }

  /**
   * Lists checklist items as a page.
   *
   * @param pagination the pagination and sort input
   * @return a page of ChecklistItem objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "items")
  @PreAuthorize("@permissions.has('graphql.echo.items')")
  public PagedChecklistItems items(PaginationInput pagination) {
    return checklistItemGraphQLService.items(pagination);
  }

  /**
   * Locates a checklist item by id.
   *
   * @param id the item id
   * @return the ChecklistItem object
   */
  @DgsData(parentType = "ChecklistQueries", field = "item")
  @PreAuthorize("@permissions.has('graphql.echo.item')")
  public ChecklistItem item(String id) {
    return checklistItemGraphQLService.item(id);
  }

  /**
   * Locates the detail for an item.
   *
   * @param id the item id
   * @return the ChecklistDetail object
   */
  @DgsData(parentType = "ChecklistQueries", field = "itemDetails")
  @PreAuthorize("@permissions.has('graphql.echo.itemDetails')")
  public ChecklistDetail itemDetails(String id) {
    return checklistItemGraphQLService.itemDetails(id);
  }

  /**
   * Lists all checklist categories.
   *
   * @return a list of ChecklistCategory objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "listCategories")
  @PreAuthorize("@permissions.has('graphql.echo.listCategories')")
  public java.util.List<ChecklistCategory> listCategories() {
    return checklistItemGraphQLService.listCategories();
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
  @DgsData(parentType = "ChecklistMutations", field = "createItem")
  @PreAuthorize("@permissions.has('graphql.echo.createItem')")
  public CreateItemResponse createItem(String name, String description, String categoryId, String icon) {
    return checklistItemGraphQLService.createItem(name, description, categoryId, icon);
  }

  /**
   * Creates or updates a checklist item from input.
   *
   * @param input the item input
   * @return the saved checklist item
   */
  @DgsData(parentType = "ChecklistMutations", field = "saveItem")
  @PreAuthorize("@permissions.has('graphql.echo.saveItem')")
  public SaveItemResponse saveItem(ChecklistItemInput input) {
    return checklistItemGraphQLService.saveItem(input);
  }

  /**
   * Soft-retires a checklist item.
   *
   * @param id the item id
   * @return the retired checklist item
   */
  @DgsData(parentType = "ChecklistMutations", field = "retireItem")
  @PreAuthorize("@permissions.has('graphql.echo.retireItem')")
  public RetireItemResponse retireItem(String id) {
    return checklistItemGraphQLService.retireItem(id);
  }

  /**
   * Creates a new checklist category.
   *
   * @param name the category name
   * @param description an optional description
   * @return the created checklist category
   */
  @DgsData(parentType = "ChecklistMutations", field = "createCategory")
  @PreAuthorize("@permissions.has('graphql.echo.createCategory')")
  public CreateCategoryResponse createCategory(String name, String description) {
    return checklistItemGraphQLService.createCategory(name, description);
  }

  /**
   * Creates or updates a checklist category from input.
   *
   * @param input the category input
   * @return the saved checklist category
   */
  @DgsData(parentType = "ChecklistMutations", field = "saveCategory")
  @PreAuthorize("@permissions.has('graphql.echo.saveCategory')")
  public SaveCategoryResponse saveCategory(ChecklistCategoryInput input) {
    return checklistItemGraphQLService.saveCategory(input);
  }
}

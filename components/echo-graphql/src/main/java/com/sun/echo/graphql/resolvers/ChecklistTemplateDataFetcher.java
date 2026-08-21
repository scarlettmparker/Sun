package com.sun.echo.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistTemplate;
import com.sun.echo.codegen.types.ChecklistTemplateInput;
import com.sun.echo.codegen.types.PagedChecklistTemplateItems;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.graphql.services.ChecklistTemplateGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for checklist template queries and mutations.
 */
@DgsComponent
public class ChecklistTemplateDataFetcher {

  private final ChecklistTemplateGraphQLService checklistTemplateGraphQLService;

  public ChecklistTemplateDataFetcher(ChecklistTemplateGraphQLService checklistTemplateGraphQLService) {
    this.checklistTemplateGraphQLService = checklistTemplateGraphQLService;
  }

  /**
   * Locates a checklist template by id, without its detail.
   *
   * @param id the template id
   * @return the ChecklistTemplate object
   */
  @DgsData(parentType = "ChecklistQueries", field = "template")
  @PreAuthorize("@permissions.has('graphql.echo.template')")
  public ChecklistTemplate template(String id) {
    return checklistTemplateGraphQLService.template(id);
  }

  /**
   * Locates the detail for a template.
   *
   * @param id the template id
   * @return the ChecklistDetail object
   */
  @DgsData(parentType = "ChecklistQueries", field = "templateDetails")
  @PreAuthorize("@permissions.has('graphql.echo.templateDetails')")
  public ChecklistDetail templateDetails(String id) {
    return checklistTemplateGraphQLService.templateDetails(id);
  }

  /**
   * Lists all checklist templates.
   *
   * @return a list of ChecklistTemplate objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "listTemplates")
  @PreAuthorize("@permissions.has('graphql.echo.listTemplates')")
  public List<ChecklistTemplate> listTemplates() {
    return checklistTemplateGraphQLService.listTemplates();
  }

  /**
   * Lists the items belonging to a template as a page.
   *
   * @param templateId the template id
   * @param pagination the pagination and sort input
   * @return a page of ChecklistTemplateItem objects
   */
  @DgsData(parentType = "ChecklistQueries", field = "templateItems")
  @PreAuthorize("@permissions.has('graphql.echo.templateItems')")
  public PagedChecklistTemplateItems templateItems(String templateId, PaginationInput pagination) {
    return checklistTemplateGraphQLService.templateItems(templateId, pagination);
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
  @PreAuthorize("@permissions.has('graphql.echo.createTemplate')")
  public QueryResult createTemplate(String name, String description, List<String> itemIds) {
    return checklistTemplateGraphQLService.createTemplate(name, description, itemIds);
  }

  /**
   * Creates or updates a checklist template from input.
   *
   * @param input the template input
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "saveTemplate")
  @PreAuthorize("@permissions.has('graphql.echo.saveTemplate')")
  public QueryResult saveTemplate(ChecklistTemplateInput input) {
    return checklistTemplateGraphQLService.saveTemplate(input);
  }

  /**
   * Archives a checklist template.
   *
   * @param id the template id
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "archiveTemplate")
  @PreAuthorize("@permissions.has('graphql.echo.archiveTemplate')")
  public QueryResult archiveTemplate(String id) {
    return checklistTemplateGraphQLService.archiveTemplate(id);
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
  @PreAuthorize("@permissions.has('graphql.echo.addTemplateItem')")
  public QueryResult addTemplateItem(String templateId, String itemId, Integer position) {
    return checklistTemplateGraphQLService.addTemplateItem(templateId, itemId, position);
  }

  /**
   * Removes an item from a template.
   *
   * @param templateId the template id
   * @param itemId the item id
   * @return a QueryResult
   */
  @DgsData(parentType = "ChecklistMutations", field = "removeTemplateItem")
  @PreAuthorize("@permissions.has('graphql.echo.removeTemplateItem')")
  public QueryResult removeTemplateItem(String templateId, String itemId) {
    return checklistTemplateGraphQLService.removeTemplateItem(templateId, itemId);
  }
}

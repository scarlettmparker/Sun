package com.sun.echo.graphql.services;

import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.codegen.types.ChecklistTemplate;
import com.sun.echo.codegen.types.ChecklistTemplateInput;
import com.sun.echo.codegen.types.ChecklistTemplateItem;
import com.sun.echo.codegen.types.PagedChecklistTemplateItems;
import com.sun.echo.codegen.types.PaginationInput;
import com.sun.echo.codegen.types.QueryResult;
import com.sun.echo.codegen.types.QuerySuccess;
import com.sun.echo.codegen.types.StandardError;
import com.sun.echo.graphql.mappers.ChecklistDetailMapper;
import com.sun.echo.graphql.mappers.ChecklistTemplateItemMapper;
import com.sun.echo.graphql.mappers.ChecklistTemplateMapper;
import com.sun.echo.model.ChecklistTemplateEntity;
import com.sun.echo.model.ChecklistTemplateItemEntity;
import com.sun.echo.service.ChecklistDetailService;
import com.sun.echo.service.ChecklistTemplateItemService;
import com.sun.echo.service.ChecklistTemplateService;
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
 * GraphQL business logic for checklist templates.
 */
@Service
public class ChecklistTemplateGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ChecklistTemplateGraphQLService.class);

  private final ChecklistTemplateService templateService;
  private final ChecklistTemplateItemService templateItemService;
  private final ChecklistDetailService detailService;

  private final ChecklistTemplateMapper templateMapper;
  private final ChecklistTemplateItemMapper templateItemMapper;
  private final ChecklistDetailMapper detailMapper;

  public ChecklistTemplateGraphQLService(ChecklistTemplateService templateService,
      ChecklistTemplateItemService templateItemService, ChecklistDetailService detailService,
      ChecklistTemplateMapper templateMapper, ChecklistTemplateItemMapper templateItemMapper,
      ChecklistDetailMapper detailMapper) {
    this.templateService = templateService;
    this.templateItemService = templateItemService;
    this.detailService = detailService;
    this.templateMapper = templateMapper;
    this.templateItemMapper = templateItemMapper;
    this.detailMapper = detailMapper;
  }

  /**
   * Locates a template by id without its detail sidecar.
   *
   * @param id the template id
   * @return the GraphQL ChecklistTemplate, or null if not found
   */
  @Transactional(readOnly = true)
  public ChecklistTemplate template(String id) {
    return templateService.locate(UUID.fromString(id)).map(templateMapper::map).orElse(null);
  }

  /**
   * Locates the detail sidecar for a template.
   *
   * @param id the template id
   * @return the GraphQL ChecklistDetail, or null if none exists
   */
  @Transactional(readOnly = true)
  public ChecklistDetail templateDetails(String id) {
    return detailService.findTemplateDetail(UUID.fromString(id)).map(detailMapper::map).orElse(null);
  }

  /**
   * Lists every checklist template.
   *
   * @return the GraphQL ChecklistTemplates
   */
  @Transactional(readOnly = true)
  public List<ChecklistTemplate> listTemplates() {
    return templateService.findAll().stream().map(templateMapper::map).collect(Collectors.toList());
  }

  /**
   * Lists the items belonging to a template as a page, sorted by position by default.
   *
   * @param templateId the template id
   * @param pagination the pagination and sort input
   * @return a page of template items
   */
  @Transactional(readOnly = true)
  public PagedChecklistTemplateItems templateItems(String templateId, PaginationInput pagination) {
    Page<ChecklistTemplateItemEntity> result = templateItemService
        .listForTemplatePaged(UUID.fromString(templateId),
            EchoGraphQLSupport.toPageable(pagination, "position", Sort.Direction.ASC));
    List<ChecklistTemplateItem> items = templateItemMapper.map(result.getContent());
    return PagedChecklistTemplateItems.newBuilder()
        .items(items)
        .pageInfo(EchoGraphQLSupport.pageInfo(result))
        .build();
  }

  /**
   * Creates a template, optionally seeded with pre-selected items.
   *
   * @param name the template name
   * @param description an optional description
   * @param itemIds optional item ids to seed
   * @return a QueryResult
   */
  @Transactional
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
  @Transactional
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
  @Transactional
  public QueryResult archiveTemplate(String id) {
    return mutate("archiveTemplate", () -> templateService.archive(UUID.fromString(id)).getId());
  }

  /**
   * Adds an item to a template, auto-positioning when position is null.
   *
   * @param templateId the template id
   * @param itemId the item id
   * @param position an optional explicit position
   * @return a QueryResult
   */
  @Transactional
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
  @Transactional
  public QueryResult removeTemplateItem(String templateId, String itemId) {
    UUID templateUuid = UUID.fromString(templateId);
    return mutate("removeTemplateItem", () -> {
      templateItemService.removeTemplateItem(templateUuid, UUID.fromString(itemId));
      return templateUuid;
    });
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

package com.sun.hades.graphql.services;

import com.sun.base.util.FilterSpec;
import com.sun.base.util.GraphQLSupport;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.codegen.types.PagedReaderTexts;
import com.sun.hades.codegen.types.PagedTextViews;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.codegen.types.StandardError;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.codegen.types.TextLevelAssessment;
import com.sun.hades.codegen.types.TextVersion;
import com.sun.hades.codegen.types.TextView;
import com.sun.hades.graphql.inference.InferenceClient;
import com.sun.hades.graphql.mappers.ReaderSourceMapper;
import com.sun.hades.graphql.mappers.ReaderTextMapper;
import com.sun.hades.graphql.mappers.TextVersionMapper;
import com.sun.hades.model.ReaderSourceEntity;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.TextVersionEntity;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.service.ReaderSourceService;
import com.sun.hades.service.ReaderTextService;
import com.sun.hades.service.TextViewService;
import com.sun.hades.service.TextVersionService;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for the reader.
 */
@Service
public class ReaderTextGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ReaderTextGraphQLService.class);

  private final ReaderTextService textService;
  private final ReaderSourceService sourceService;
  private final ReaderTextMapper textMapper;
  private final ReaderSourceMapper sourceMapper;
  private final InferenceClient inferenceClient;
  private final TextViewService textViewService;
  private final TextVersionService textVersionService;
  private final TextVersionMapper textVersionMapper;

  public ReaderTextGraphQLService(ReaderTextService textService, ReaderSourceService sourceService,
      ReaderTextMapper textMapper, ReaderSourceMapper sourceMapper, InferenceClient inferenceClient,
      TextViewService textViewService, TextVersionService textVersionService,
      TextVersionMapper textVersionMapper) {
    this.textService = textService;
    this.sourceService = sourceService;
    this.textMapper = textMapper;
    this.sourceMapper = sourceMapper;
    this.inferenceClient = inferenceClient;
    this.textViewService = textViewService;
    this.textVersionService = textVersionService;
    this.textVersionMapper = textVersionMapper;
  }

  /**
   * Lists texts, optionally filtered by level, source, and type.
   *
   * @param level optional CEFR level filter
   * @param sourceId optional source id filter
   * @param ownerId optional owner account id filter
   * @param pagination the pagination and sort input
   * @return a page of texts
   */
  @Cacheable("texts")
  @Transactional(readOnly = true)
  public PagedReaderTexts texts(PaginationInput pagination) {
    Pageable pageable = HadesGraphQLSupport.toPageable(pagination, "level", Sort.Direction.ASC);
    List<FilterSpec> filters = GraphQLSupport.toFilterSpecs(
        pagination == null ? null : pagination.getFilters(),
        f -> new FilterSpec(f.getField(), f.getOperator().name(), f.getValue()));
    Page<ReaderTextEntity> result = textService.list(filters, pageable);
    List<ReaderText> items = result.getContent().stream().map(textMapper::map).toList();
    return PagedReaderTexts.newBuilder().items(items).pageInfo(HadesGraphQLSupport.pageInfo(result)).build();
  }

  /**
   * Locates a text by id.
   *
   * @param id the text id
   * @return the text, or null
   */
  @Transactional(readOnly = true)
  public ReaderText text(String id) {
    ReaderText result = textService.findById(UUID.fromString(id)).map(textMapper::map).orElse(null);
    if (result != null) {
      try {
        textViewService.markViewed(UUID.fromString(id));
      } catch (Exception e) {
        logger.debug("Failed to mark viewed for text {}", id, e);
      }
    }
    return result;
  }

  /**
   * Lists viewed texts for the caller.
   *
   * @param pagination the pagination input
   * @return a page of views
   */
  @Transactional(readOnly = true)
  public PagedTextViews viewedTexts(PaginationInput pagination) {
    Pageable pageable = HadesGraphQLSupport.toPageable(pagination, "viewedAt", Sort.Direction.DESC);
    Page<TextView> items =
        textViewService.viewedTexts(pageable).map(v ->
            TextView.newBuilder()
                .textId(v.getTextId().toString())
                .viewedAt(v.getViewedAt().atOffset(ZoneOffset.UTC))
                .build());
    return PagedTextViews.newBuilder()
        .items(items.getContent())
        .pageInfo(HadesGraphQLSupport.pageInfo(items))
        .build();
  }

  /**
   * Lists versions for a text.
   *
   * @param textId the text id
   * @return the versions
   */
  @Transactional(readOnly = true)
  public List<TextVersion> textVersions(String textId) {
    UUID id = UUID.fromString(textId);
    return textVersionService.listForText(id).stream()
        .map(textVersionMapper::map)
        .toList();
  }

  /**
   * Edits a text.
   *
   * @param id the text id
   * @param input the new values
   * @return a QueryResult
   */
  @Transactional
  public QueryResult editText(String id, ReaderTextInput input) {
    return mutate("editText", () ->
        textService.editText(
            UUID.fromString(id),
            input.getTitle(),
            input.getContent(),
            input.getLanguage(),
            input.getLevel(),
            input.getSourceId()));
  }

  /**
   * Marks a text as viewed.
   *
   * @param textId the text id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult markViewed(String textId) {
    return mutate("markViewed", () -> {
      textViewService.markViewed(UUID.fromString(textId));
      return UUID.fromString(textId);
    });
  }

  /**
   * Returns just the text body, fetched on demand by the lazy content resolver.
   *
   * @param id the text id
   * @return the content, or null when the text is missing
   */
  @Transactional(readOnly = true)
  public String textContent(String id) {
    return textService.findById(UUID.fromString(id))
        .map(ReaderTextEntity::getContent)
        .orElse(null);
  }

  /**
   * Predicts the CEFR level of a text via the inference service.
   *
   * @param text the text to classify
   * @return the assessment, or null when the service is unavailable
   */
  @Transactional(readOnly = true)
  public TextLevelAssessment classifyTextLevel(String text) {
    return inferenceClient.classify(text).orElse(null);
  }

  /**
   * Locates a source by id.
   *
   * @param id the source id
   * @return the source, or null
   */
  @Transactional(readOnly = true)
  public ReaderSource source(String id) {
    return sourceService.findById(UUID.fromString(id)).map(sourceMapper::map).orElse(null);
  }

  /**
   * Lists all sources.
   *
   * @return the sources
   */
  @Transactional(readOnly = true)
  public List<ReaderSource> sources() {
    return sourceService.findAll().stream().map(sourceMapper::map).toList();
  }

  /**
   * Creates a source.
   *
   * @param name the source name
   * @param url the source url
   * @return a QueryResult
   */
  @Transactional
  public QueryResult createSource(String name, String url) {
    return mutate("createSource", () -> {
      requireUser();
      ReaderSourceEntity entity = new ReaderSourceEntity();
      entity.setName(name);
      entity.setUrl(url);
      return sourceService.save(entity).getId();
    });
  }

  /**
   * Creates a text.
   *
   * @param input the text input
   * @return a QueryResult
   */
  @Transactional
  public QueryResult createText(ReaderTextInput input) {
    return mutate("createText", () -> {
      requireUser();
      ReaderTextEntity entity = textMapper.mapInput(input);
      return textService.save(entity).getId();
    });
  }

  /**
   * Archives a text.
   *
   * @param id the text id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult archiveText(String id) {
    return mutate("archiveText", () -> {
      requireUser();
      ReaderTextEntity text = textService.findById(UUID.fromString(id))
          .orElseThrow(() -> new IllegalArgumentException("Text not found: " + id));
      text.setStatus(ReaderTextStatus.ARCHIVED);
      return textService.save(text).getId();
    });
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

  /**
   * Returns the authenticated account id, throwing if none is present.
   *
   * @return the caller's account id
   */
  private UUID requireUser() {
    UUID id = UserContextHolder.getUserId();
    if (id == null) {
      throw new IllegalArgumentException("Authentication required");
    }
    return id;
  }
}

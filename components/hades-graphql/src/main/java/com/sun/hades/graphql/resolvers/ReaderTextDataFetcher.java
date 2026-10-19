package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.base.audit.annotation.Audited;
import com.sun.base.audit.annotation.NotAudited;
import com.sun.base.util.FilterSpec;
import com.sun.base.util.GraphQLSupport;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.codegen.types.ArchiveTextResponse;
import com.sun.hades.codegen.types.CreateSourceResponse;
import com.sun.hades.codegen.types.CreateTextResponse;
import com.sun.hades.codegen.types.HadesMutations;
import com.sun.hades.codegen.types.HadesQueries;
import com.sun.hades.codegen.types.MarkViewedResponse;
import com.sun.hades.codegen.types.PagedReaderTexts;
import com.sun.hades.codegen.types.PagedTextViews;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.codegen.types.TextLevelAssessment;
import com.sun.hades.codegen.types.TextView;
import com.sun.hades.codegen.types.Word;
import com.sun.hades.codegen.types.WordDictionary;
import com.sun.hades.codegen.types.WordScope;
import com.sun.hades.graphql.inference.InferenceClient;
import com.sun.hades.graphql.mappers.ReaderSourceMapper;
import com.sun.hades.graphql.mappers.ReaderTextMapper;
import com.sun.hades.graphql.services.HadesGraphQLSupport;
import com.sun.hades.graphql.services.WordReferenceService;
import com.sun.hades.model.ReaderSourceEntity;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.TextViewEntity;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.repository.ReaderTextRepository;
import com.sun.hades.service.ReaderSourceService;
import com.sun.hades.service.ReaderTextService;
import com.sun.hades.service.TextViewService;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for the reader queries and mutations.
 */
@DgsComponent
public class ReaderTextDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(ReaderTextDataFetcher.class);

  private final ReaderTextService textService;
  private final ReaderSourceService sourceService;
  private final ReaderTextMapper textMapper;
  private final ReaderSourceMapper sourceMapper;
  private final InferenceClient inferenceClient;
  private final TextViewService textViewService;
  private final ReaderTextRepository textRepository;
  private final WordReferenceService wordReferenceService;

  public ReaderTextDataFetcher(ReaderTextService textService, ReaderSourceService sourceService,
      ReaderTextMapper textMapper, ReaderSourceMapper sourceMapper, InferenceClient inferenceClient,
      TextViewService textViewService, ReaderTextRepository textRepository,
      WordReferenceService wordReferenceService) {
    this.textService = textService;
    this.sourceService = sourceService;
    this.textMapper = textMapper;
    this.sourceMapper = sourceMapper;
    this.inferenceClient = inferenceClient;
    this.textViewService = textViewService;
    this.textRepository = textRepository;
    this.wordReferenceService = wordReferenceService;
  }

  /**
   * Provides the reader queries object.
   *
   * @return a new HadesQueries instance
   */
  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Query", field = "hadesQueries")
  public HadesQueries getHadesQueries() {
    return HadesQueries.newBuilder().build();
  }

  /**
   * Provides the reader mutations object.
   *
   * @return a new HadesMutations instance
   */
  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Mutation", field = "hadesMutations")
  public HadesMutations getHadesMutations() {
    return HadesMutations.newBuilder().build();
  }

  /**
   * Lists texts, optionally filtered.
   *
   * @param level optional CEFR level
   * @param sourceId optional source id
   * @param ownerId optional owner account id
   * @param pagination the pagination input
   * @return a page of texts
   */
  @Audited(event = "READERTEXT_LIST", entity = "ReaderText")
  @DgsData(parentType = "HadesQueries", field = "texts")
  @PreAuthorize("@permissions.has('graphql.hades.texts')")
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
   * @return the text
   */
  @Audited(event = "READERTEXT_VIEW", entity = "ReaderText")
  @DgsData(parentType = "HadesQueries", field = "text")
  @PreAuthorize("@permissions.has('graphql.hades.text')")
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
   * Resolves the heavy text body only when the client selects it, so list
   * queries that ask for id/title don't pull and serialize the full content.
   *
   * @param env the data-fetching environment, providing the parent ReaderText
   * @return the text content, or null when the parent has no id
   */
  @NotAudited(reason = "field resolver")
  @DgsData(parentType = "ReaderText", field = "content")
  @PreAuthorize("@permissions.has('graphql.hades.textContent')")
  @Transactional(readOnly = true)
  public String textContent(DgsDataFetchingEnvironment env) {
    ReaderText parent = env.getSource();
    if (parent == null || parent.getId() == null) {
      return null;
    }
    return textService.findById(UUID.fromString(parent.getId()))
        .map(ReaderTextEntity::getContent)
        .orElse(null);
  }

  /**
   * Predicts the CEFR level of a text.
   *
   * @param text the text to classify
   * @return the assessment
   */
  @Audited(event = "READERTEXT_CLASSIFY", entity = "ReaderText")
  @DgsData(parentType = "HadesQueries", field = "classifyTextLevel")
  @PreAuthorize("@permissions.has('graphql.hades.classifyTextLevel')")
  @Transactional(readOnly = true)
  public TextLevelAssessment classifyTextLevel(String text) {
    return inferenceClient.classify(text).orElse(null);
  }

  /**
   * Defines a word from WordReference, honoring the requested scopes.
   *
   * @param word the headword to look up
   * @param scope the parts of the page to include
   * @param dictionary the dictionary to query, defaulting to Greek-English
   * @return the word, or null when the entry does not exist
   */
  @Audited(event = "WORD_DEFINE")
  @DgsData(parentType = "HadesQueries", field = "defineWord")
  @PreAuthorize("@permissions.has('graphql.hades.defineWord')")
  public Word defineWord(String word, List<WordScope> scope, WordDictionary dictionary) {
    return wordReferenceService.defineWord(word, scope, dictionary);
  }

  /**
   * Locates a source by id.
   *
   * @param id the source id
   * @return the source
   */
  @Audited(event = "READERSOURCE_VIEW", entity = "ReaderSource")
  @DgsData(parentType = "HadesQueries", field = "source")
  @PreAuthorize("@permissions.has('graphql.hades.source')")
  @Transactional(readOnly = true)
  public ReaderSource source(String id) {
    return sourceService.findById(UUID.fromString(id)).map(sourceMapper::map).orElse(null);
  }

  /**
   * Lists all sources.
   *
   * @return the sources
   */
  @Audited(event = "READERSOURCE_LIST", entity = "ReaderSource")
  @DgsData(parentType = "HadesQueries", field = "sources")
  @PreAuthorize("@permissions.has('graphql.hades.sources')")
  @Transactional(readOnly = true)
  public List<ReaderSource> sources() {
    return sourceService.findAll().stream().map(sourceMapper::map).toList();
  }

  /**
   * Creates a source.
   *
   * @param name the source name
   * @param url the source url
   * @return the create-source response
   */
  @Audited(event = "READERSOURCE_CREATE", entity = "ReaderSource")
  @DgsData(parentType = "HadesMutations", field = "createSource")
  @PreAuthorize("@permissions.has('graphql.hades.createSource')")
  @Transactional
  public CreateSourceResponse createSource(String name, String url) {
    requireUser();
    ReaderSourceEntity entity = new ReaderSourceEntity();
    entity.setName(name);
    entity.setUrl(url);
    ReaderSourceEntity saved = sourceService.save(entity);
    logger.info("createSource succeeded for id {}", saved.getId());
    return CreateSourceResponse.newBuilder()
        .message("Source created successfully")
        .source(sourceMapper.map(saved))
        .build();
  }

  /**
   * Creates a text.
   *
   * @param input the text input
   * @return the create-text response
   */
  @Audited(event = "READERTEXT_CREATE", entity = "ReaderText", sensitiveFields = {"content"})
  @DgsData(parentType = "HadesMutations", field = "createText")
  @PreAuthorize("@permissions.has('graphql.hades.createText')")
  @Transactional
  public CreateTextResponse createText(ReaderTextInput input) {
    requireUser();
    ReaderTextEntity entity = textMapper.mapInput(input);
    ReaderTextEntity saved = textService.save(entity);
    logger.info("createText succeeded for id {}", saved.getId());
    return CreateTextResponse.newBuilder()
        .message("Text created successfully")
        .text(textMapper.map(saved))
        .build();
  }

  /**
   * Archives a text.
   *
   * @param id the text id
   * @return the archive-text response
   */
  @Audited(event = "READERTEXT_ARCHIVE", entity = "ReaderText")
  @DgsData(parentType = "HadesMutations", field = "archiveText")
  @PreAuthorize("@permissions.has('graphql.hades.archiveText')")
  @Transactional
  public ArchiveTextResponse archiveText(String id) {
    requireUser();
    ReaderTextEntity text = textService.findById(UUID.fromString(id))
        .orElseThrow(() -> new IllegalArgumentException("Text not found: " + id));
    text.setStatus(ReaderTextStatus.ARCHIVED);
    ReaderTextEntity saved = textService.save(text);
    logger.info("archiveText succeeded for id {}", saved.getId());
    return ArchiveTextResponse.newBuilder()
        .message("Text archived successfully")
        .text(textMapper.map(saved))
        .build();
  }

  /**
   * Marks a text as viewed.
   *
   * @param textId the text id
   * @return the mark-viewed response
   */
  @Audited(event = "TEXTVIEW_MARK", entity = "TextView")
  @DgsData(parentType = "HadesMutations", field = "markViewed")
  @PreAuthorize("@permissions.has('graphql.hades.markViewed')")
  @Transactional
  public MarkViewedResponse markViewed(String textId) {
    textViewService.markViewed(UUID.fromString(textId));
    logger.info("markViewed succeeded for id {}", textId);
    return MarkViewedResponse.newBuilder()
        .message("Text marked as viewed")
        .textId(textId)
        .build();
  }

  /**
   * Lists viewed texts.
   *
   * @param pagination the pagination
   * @return a page of views
   */
  @Audited(event = "TEXTVIEW_LIST", entity = "TextView")
  @DgsData(parentType = "HadesQueries", field = "viewedTexts")
  @PreAuthorize("@permissions.has('graphql.hades.viewedTexts')")
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
   * Lists the full texts the caller has viewed, in viewed-at order.
   *
   * @param pagination the pagination
   * @return a page of texts
   */
  @Audited(event = "READERTEXT_LISTVIEWED", entity = "ReaderText")
  @DgsData(parentType = "HadesQueries", field = "viewedReaderTexts")
  @PreAuthorize("@permissions.has('graphql.hades.viewedReaderTexts')")
  @Transactional(readOnly = true)
  public PagedReaderTexts viewedReaderTexts(PaginationInput pagination) {
    Pageable pageable = HadesGraphQLSupport.toPageable(pagination, "viewedAt", Sort.Direction.DESC);
    Page<TextViewEntity> views = textViewService.viewedTexts(pageable);
    List<UUID> ids = views.getContent().stream()
        .map(TextViewEntity::getTextId)
        .toList();
    if (ids.isEmpty()) {
      return PagedReaderTexts.newBuilder()
          .items(List.of())
          .pageInfo(HadesGraphQLSupport.pageInfo(views))
          .build();
    }
    List<ReaderTextEntity> entities = textRepository.findAllById(ids);
    Map<UUID, ReaderTextEntity> byId = entities.stream()
        .collect(Collectors.toMap(ReaderTextEntity::getId, Function.identity()));
    List<ReaderText> ordered = ids.stream()
        .map(byId::get)
        .filter(Objects::nonNull)
        .map(textMapper::map)
        .toList();
    return PagedReaderTexts.newBuilder()
        .items(ordered)
        .pageInfo(HadesGraphQLSupport.pageInfo(views))
        .build();
  }

  /**
   * Locates reader texts by ids for batch attach resolution.
   *
   * @param ids the text ids
   * @return the texts
   */
  @Audited(event = "READERTEXT_LOCATE", entity = "ReaderText")
  @DgsData(parentType = "HadesQueries", field = "locateReaderTexts")
  @PreAuthorize("@permissions.has('graphql.hades.locateReaderTexts')")
  @Transactional(readOnly = true)
  public List<ReaderText> locateReaderTexts(List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    List<UUID> uuids = ids.stream()
        .map(id -> {
          try {
            return UUID.fromString(id);
          } catch (IllegalArgumentException e) {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .toList();
    if (uuids.isEmpty()) {
      return List.of();
    }
    List<ReaderTextEntity> entities = textRepository.findAllById(uuids);
    Map<UUID, ReaderTextEntity> byId = entities.stream()
        .collect(Collectors.toMap(ReaderTextEntity::getId, Function.identity()));
    return uuids.stream()
        .map(byId::get)
        .filter(Objects::nonNull)
        .filter(e -> e.getStatus() == ReaderTextStatus.ACTIVE)
        .map(textMapper::map)
        .toList();
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

package com.sun.hades.graphql.services;

import com.sun.base.util.GraphQLSupport;
import com.sun.base.util.PageRequests;
import com.sun.base.util.FilterSpec;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.model.enums.AccountType;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.codegen.types.CommentInput;
import com.sun.hades.codegen.types.DiscordLoginResult;
import com.sun.hades.codegen.types.PageInfo;
import com.sun.hades.codegen.types.PagedPrivateNotes;
import com.sun.hades.codegen.types.PagedReaderAnnotations;
import com.sun.hades.codegen.types.PagedReaderComments;
import com.sun.hades.codegen.types.PagedReaderTexts;
import com.sun.hades.codegen.types.PrivateNote;
import com.sun.hades.codegen.types.PrivateNoteInput;
import com.sun.hades.codegen.types.ShareNotesInput;
import com.sun.hades.codegen.types.TextLevelAssessment;
import com.sun.hades.graphql.inference.InferenceClient;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderComment;
import com.sun.hades.codegen.types.ReaderPosition;
import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.codegen.types.ReaderObjectReference;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.codegen.types.RemoteUserInput;
import com.sun.hades.codegen.types.RemoteUserType;
import com.sun.hades.codegen.types.StandardError;
import com.sun.hades.codegen.types.VoteInput;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import com.sun.hades.graphql.mappers.PrivateNoteMapper;
import com.sun.hades.graphql.mappers.ReaderAccountMapper;
import com.sun.hades.graphql.mappers.ReaderAnnotationMapper;
import com.sun.hades.graphql.mappers.ReaderCommentMapper;
import com.sun.hades.graphql.mappers.ReaderObjectReferenceMapper;
import com.sun.hades.graphql.mappers.ReaderPositionMapper;
import com.sun.hades.graphql.mappers.ReaderSourceMapper;
import com.sun.hades.graphql.mappers.ReaderTextMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.model.PrivateNoteEntity;
import com.sun.hades.model.ReaderAccountEntity;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderCommentEntity;
import com.sun.hades.model.ReaderPositionEntity;
import com.sun.hades.model.ReaderSourceEntity;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.service.DiscordOAuthService;
import com.sun.hades.service.PrivateNoteService;
import com.sun.hades.service.ReaderAccountService;
import com.sun.hades.service.RemoteObjectReference;
import com.sun.hades.service.ReaderAnnotationService;
import com.sun.hades.service.ReaderCommentService;
import com.sun.hades.service.ReaderPositionService;
import com.sun.hades.service.ReaderSourceService;
import com.sun.hades.service.ReaderTextService;
import com.sun.hades.service.ReaderVoteService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sun.hades.model.enums.ReaderStatus;

/**
 * GraphQL business logic for the reader.
 */
@Service
public class HadesGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(HadesGraphQLService.class);

  private final ReaderTextService textService;
  private final ReaderSourceService sourceService;
  private final ReaderAnnotationService annotationService;
  private final ReaderCommentService commentService;
  private final ReaderVoteService voteService;
  private final ReaderAccountService accountService;
  private final ReaderPositionService positionService;
  private final PrivateNoteService privateNoteService;
  private final DiscordOAuthService discordOAuthService;
  private final AccountService gaiaAccountService;
  private final JwtService jwtService;

  private final ReaderTextMapper textMapper;
  private final ReaderSourceMapper sourceMapper;
  private final ReaderAnnotationMapper annotationMapper;
  private final ReaderCommentMapper commentMapper;
  private final PrivateNoteMapper privateNoteMapper;
  private final ReaderAccountMapper accountMapper;
  private final ReaderPositionMapper positionMapper;
  private final ReaderObjectReferenceMapper objectReferenceMapper;
  private final RemoteUserMapper remoteUserMapper;
  private final InferenceClient inferenceClient;

  public HadesGraphQLService(ReaderTextService textService, ReaderSourceService sourceService,
      ReaderAnnotationService annotationService, ReaderCommentService commentService,
      ReaderVoteService voteService, ReaderAccountService accountService,
      ReaderPositionService positionService, PrivateNoteService privateNoteService,
      DiscordOAuthService discordOAuthService,
      AccountService gaiaAccountService, JwtService jwtService, ReaderTextMapper textMapper,
      ReaderSourceMapper sourceMapper, ReaderAnnotationMapper annotationMapper,
      ReaderCommentMapper commentMapper, PrivateNoteMapper privateNoteMapper,
      ReaderAccountMapper accountMapper,
      ReaderPositionMapper positionMapper, ReaderObjectReferenceMapper objectReferenceMapper,
      RemoteUserMapper remoteUserMapper, InferenceClient inferenceClient) {
    this.textService = textService;
    this.sourceService = sourceService;
    this.annotationService = annotationService;
    this.commentService = commentService;
    this.voteService = voteService;
    this.accountService = accountService;
    this.positionService = positionService;
    this.privateNoteService = privateNoteService;
    this.discordOAuthService = discordOAuthService;
    this.gaiaAccountService = gaiaAccountService;
    this.jwtService = jwtService;
    this.textMapper = textMapper;
    this.sourceMapper = sourceMapper;
    this.annotationMapper = annotationMapper;
    this.commentMapper = commentMapper;
    this.privateNoteMapper = privateNoteMapper;
    this.accountMapper = accountMapper;
    this.positionMapper = positionMapper;
    this.objectReferenceMapper = objectReferenceMapper;
    this.remoteUserMapper = remoteUserMapper;
    this.inferenceClient = inferenceClient;
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
    Pageable pageable = toPageable(pagination, "level", Sort.Direction.ASC);
    List<FilterSpec> filters = GraphQLSupport.toFilterSpecs(
        pagination == null ? null : pagination.getFilters(),
        f -> new FilterSpec(f.getField(), f.getOperator().name(), f.getValue()));
    Page<ReaderTextEntity> result = textService.list(filters, pageable);
    List<ReaderText> items = result.getContent().stream().map(textMapper::map).toList();
    return PagedReaderTexts.newBuilder().items(items).pageInfo(pageInfo(result)).build();
  }

  /**
   * Locates a text by id.
   *
   * @param id the text id
   * @return the text, or null
   */
  @Transactional(readOnly = true)
  public ReaderText text(String id) {
    return textService.findById(UUID.fromString(id)).map(textMapper::map).orElse(null);
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
   * Paginated annotations for a text, optionally including hidden ones.
   *
   * @param textId the text id
   * @param includeHidden whether to include hidden annotations
   * @param pagination the page request
   * @return the paged annotations
   */
  @Transactional(readOnly = true)
  public PagedReaderAnnotations annotations(
      String textId, Boolean includeHidden, PaginationInput pagination) {
    UUID id = UUID.fromString(textId);
    Pageable pageable = toPageable(pagination, "createdAt", Sort.Direction.DESC);
    var page = annotationService.listForTextPaged(
        id, Boolean.TRUE.equals(includeHidden), pageable);
    Map<UUID, ReaderPosition> positions =
        positionService.listForText(id).stream()
            .collect(Collectors.toMap(
                ReaderPositionEntity::getId, positionMapper::map, (a, b) -> a));
    Map<UUID, RemoteUser> authors = new HashMap<>();
    List<UUID> authorIds = page.getContent().stream()
        .map(ReaderAnnotationEntity::getCreatedBy)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    accountService.findByGaiaAccountIdIn(authorIds).forEach(acc ->
        authors.put(acc.getGaiaAccountId(), remoteUserMapper.discord(acc.getDiscordId())));
    Map<UUID, VoteValue> myVotes = voteService.myVotes(
        ReaderVoteTarget.ANNOTATION,
        page.getContent().stream().map(ReaderAnnotationEntity::getId).toList());
    Map<UUID, Long> replyCounts = commentService.countByAnnotationIds(
        page.getContent().stream().map(ReaderAnnotationEntity::getId).toList());
    List<ReaderAnnotation> items = page.getContent().stream()
        .map(a -> annotationMapper.map(a, positions.get(a.getPositionId()),
            authors.get(a.getCreatedBy()),
            replyCounts.getOrDefault(a.getId(), 0L).intValue(),
            myVotes.get(a.getId())))
        .toList();
    return PagedReaderAnnotations.newBuilder()
        .items(items)
        .pageInfo(pageInfo(page))
        .build();
  }

  /**
   * Locates an annotation by id.
   *
   * @param id the annotation id
   * @return the annotation, or null
   */
  @Transactional(readOnly = true)
  public ReaderAnnotation annotation(String id) {
    UUID annotationId = UUID.fromString(id);
    return annotationService.findById(annotationId)
        .map(a -> {
          int replyCount = commentService
              .countByAnnotationIds(List.of(annotationId))
              .getOrDefault(annotationId, 0L)
              .intValue();
          return annotationMapper.map(a, null, null, replyCount, null);
        })
        .orElse(null);
  }

  /**
   * Lists comments for an annotation.
   *
   * @param annotationId the annotation id
   * @param includeHidden whether to include hidden comments
   * @param pagination the pagination input
   * @return a page of comments
   */
  @Transactional(readOnly = true)
  public PagedReaderComments comments(
      String annotationId, Boolean includeHidden, PaginationInput pagination) {
    Pageable pageable = toPageable(pagination, "createdAt", Sort.Direction.ASC);
    Page<ReaderCommentEntity> result =
        commentService.listForAnnotation(UUID.fromString(annotationId), pageable);
    List<ReaderCommentEntity> visible = result.getContent().stream()
        .filter(c -> Boolean.TRUE.equals(includeHidden) || c.getStatus() == ReaderStatus.ACTIVE)
        .toList();
    Map<UUID, RemoteUser> authors = new HashMap<>();
    List<UUID> authorIds = visible.stream()
        .map(ReaderCommentEntity::getCreatedBy)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    accountService.findByGaiaAccountIdIn(authorIds).forEach(acc ->
        authors.put(acc.getGaiaAccountId(), remoteUserMapper.discord(acc.getDiscordId())));
    Map<UUID, VoteValue> myVotes = voteService.myVotes(
        ReaderVoteTarget.COMMENT,
        visible.stream().map(ReaderCommentEntity::getId).toList());
    List<ReaderComment> items = visible.stream()
        .map(c -> commentMapper.map(c, authors.get(c.getCreatedBy()), myVotes.get(c.getId())))
        .toList();
    return PagedReaderComments.newBuilder().items(items).pageInfo(pageInfo(result)).build();
  }

  /**
   * Paginated private notes for a text, visible to the current viewer.
   *
   * @param textId the text id
   * @param pagination the page request
   * @return the paged private notes
   */
  @Transactional(readOnly = true)
  public PagedPrivateNotes privateNotes(String textId, PaginationInput pagination) {
    UUID id = UUID.fromString(textId);
    Pageable pageable = toPageable(pagination, "createdAt", Sort.Direction.DESC);
    var page = privateNoteService.listForText(id, pageable);
    Map<UUID, RemoteUser> authors = new HashMap<>();
    List<UUID> ownerIds = page.getContent().stream()
        .map(PrivateNoteEntity::getOwnerId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    accountService.findByGaiaAccountIdIn(ownerIds).forEach(acc ->
        authors.put(acc.getGaiaAccountId(), remoteUserMapper.discord(acc.getDiscordId())));
    List<PrivateNote> items = page.getContent().stream()
        .map(n -> privateNoteMapper.map(n, authors.get(n.getOwnerId())))
        .toList();
    return PagedPrivateNotes.newBuilder().items(items).pageInfo(pageInfo(page)).build();
  }

  /**
   * Returns the current member's reader account.
   *
   * @return the reader account, or null
   */
  @Transactional(readOnly = true)
  public ReaderAccount readerAccount() {
    UUID userId = UserContextHolder.getUserId();
    if (userId == null) {
      return null;
    }
    return accountService.findByGaiaAccountId(userId).map(accountMapper::map).orElse(null);
  }

  /**
   * Locates reader accounts for a set of remote users.
   *
   * @param remoteUsers the remote-user references
   * @return the matching reader accounts
   */
  @Transactional(readOnly = true)
  public List<ReaderAccount> readerAccounts(List<RemoteUserInput> remoteUsers) {
    List<String> discordIds = remoteUsers == null ? List.of()
        : remoteUsers.stream()
            .filter(r -> r.getType() == RemoteUserType.DISCORD)
            .map(RemoteUserInput::getId)
            .distinct()
            .toList();
    return accountService.findByDiscordIds(discordIds).stream()
        .map(accountMapper::map)
        .toList();
  }

  /**
   * Searches reader accounts by username.
   *
   * @param query the username fragment
   * @param pagination the pagination input
   * @return the matching reader accounts
   */
  @Transactional(readOnly = true)
  public List<ReaderAccount> searchReaderAccounts(String query, PaginationInput pagination) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    Pageable pageable = pagination == null
        ? PageRequest.of(0, 10)
        : toPageable(pagination, "globalName", Sort.Direction.ASC);
    List<ReaderAccountEntity> entities = accountService.searchByUsername(query, pageable);
    return entities.stream()
        .filter(e -> {
          UUID gaiaId = e.getGaiaAccountId();
          if (gaiaId == null) {
            return false;
          }
          return gaiaAccountService.findById(gaiaId)
              .map(a -> a.getAccountType() == AccountType.HUMAN)
              .orElse(false);
        })
        .map(e -> accountMapper.map(e))
        .toList();
  }

  /**
   * Returns the caller's vote on a target.
   *
   * @param targetType the target type
   * @param targetId the target id
   * @return the vote value, or null
   */
  @Transactional(readOnly = true)
  public VoteValue myVote(ReaderVoteTarget targetType, String targetId) {
    return voteService.myVote(targetType, UUID.fromString(targetId)).orElse(null);
  }

  /**
   * Finds annotations and private notes that reference any of the given remote object ids.
   *
   * @param ids the remote object ids
   * @return the matching references
   */
  @Transactional(readOnly = true)
  public List<ReaderObjectReference> locateRemoteObjects(List<String> ids) {
    List<RemoteObjectReference> out = new ArrayList<>();
    out.addAll(annotationService.locateRemoteObjects(ids));
    out.addAll(privateNoteService.locateRemoteObjects(ids));
    return out.stream().map(objectReferenceMapper::map).toList();
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
   * Creates an annotation on a range, enforcing non-overlap.
   *
   * @param textId the text id
   * @param startOffset the range start
   * @param endOffset the range end
   * @param body the markdown body
   * @return a QueryResult
   */
  @Transactional
  public QueryResult createAnnotation(String textId, int startOffset, int endOffset, String body) {
    return mutate("createAnnotation", () -> annotationService.createAnnotation(
        UUID.fromString(textId), startOffset, endOffset, body));
  }

  /**
   * Updates an annotation's body.
   *
   * @param id the annotation id
   * @param body the new body
   * @return a QueryResult
   */
  @Transactional
  public QueryResult editAnnotation(String id, String body) {
    return mutate("editAnnotation",
        () -> annotationService.editAnnotation(UUID.fromString(id), body));
  }

  /**
   * Deletes an annotation.
   *
   * @param id the annotation id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult deleteAnnotation(String id) {
    return mutate("deleteAnnotation", () -> {
      annotationService.deleteAnnotation(UUID.fromString(id));
      return UUID.fromString(id);
    });
  }

  /**
   * Adds a comment to an annotation.
   *
   * @param input the comment input
   * @return a QueryResult
   */
  @Transactional
  public QueryResult addComment(CommentInput input) {
    return mutate("addComment", () -> commentService.addComment(
        UUID.fromString(input.getAnnotationId()),
        input.getParentId() == null ? null : UUID.fromString(input.getParentId()),
        input.getBody()));
  }

  /**
   * Updates a comment's body.
   *
   * @param id the comment id
   * @param body the new body
   * @return a QueryResult
   */
  @Transactional
  public QueryResult editComment(String id, String body) {
    return mutate("editComment",
        () -> commentService.editComment(UUID.fromString(id), body));
  }

  /**
   * Deletes a comment.
   *
   * @param id the comment id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult deleteComment(String id) {
    return mutate("deleteComment", () -> {
      commentService.deleteComment(UUID.fromString(id));
      return UUID.fromString(id);
    });
  }

  /**
   * Casts, toggles, or flips a vote.
   *
   * @param input the vote input
   * @return a QueryResult
   */
  @Transactional
  public QueryResult vote(VoteInput input) {
    return mutate("vote", () -> voteService.vote(
        input.getTargetType(),
        UUID.fromString(input.getTargetId()),
        input.getValue()));
  }

  /**
   * Removes the caller's vote.
   *
   * @param targetType the target type
   * @param targetId the target id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult removeVote(ReaderVoteTarget targetType, String targetId) {
    return mutate("removeVote",
        () -> voteService.removeVote(targetType, UUID.fromString(targetId)));
  }

  /**
   * Creates a private note on a range.
   *
   * @param input the private note input
   * @return a QueryResult
   */
  @Transactional
  public QueryResult createPrivateNote(PrivateNoteInput input) {
    return mutate("createPrivateNote", () -> privateNoteService.createPrivateNote(
        UUID.fromString(input.getTextId()),
        input.getStartOffset(),
        input.getEndOffset(),
        input.getBody()));
  }

  /**
   * Deletes a private note (owner only).
   *
   * @param id the note id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult deletePrivateNote(String id) {
    return mutate("deletePrivateNote", () -> {
      privateNoteService.deletePrivateNote(UUID.fromString(id));
      return UUID.fromString(id);
    });
  }

  /**
   * Shares all private notes on a text.
   *
   * @param input the share input
   * @return a QueryResult
   */
  @Transactional
  public QueryResult shareNotes(ShareNotesInput input) {
    return mutate("shareNotes", () -> privateNoteService.shareNotes(
        UUID.fromString(input.getTextId()),
        input.getSubjectIds() == null ? List.of() : input.getSubjectIds().stream()
            .map(s -> {
              try {
                return UUID.fromString(s);
              } catch (Exception e) {
                return null;
              }
            })
            .filter(s -> s != null)
            .toList(),
        input.getSubjectEmails() == null ? List.of() : input.getSubjectEmails()));
  }

  /**
   * Attaches a remote object id to an annotation.
   *
   * @param source the annotation id
   * @param target the remote object id
   * @return a QueryResult
   */
  @Transactional
  public QueryResult attachObject(String source, String target) {
    return mutate("attachObject",
        () -> annotationService.attach(UUID.fromString(source), target));
  }

  /**
   * Exchanges a Discord authorization code for a JWT, upserting the gaia account
   * and reader profile.
   *
   * <p>Deactivated accounts get no token; the result flags that a reactivation
   * email is required before the member can sign back in.
   *
   * @param code the authorization code
   * @param state the OAuth state token
   * @return the login result with the JWT
   */
  @Transactional
  public DiscordLoginResult discordLogin(String code, String state) {
    DiscordOAuthService.DiscordProfile profile = discordOAuthService.exchange(code);
    AccountEntity account = gaiaAccountService.upsertProviderAccount(
        "discord", profile.discordId(), profile.username(), profile.globalName(),
        profile.email());
    UUID readerAccountId = accountService.upsertFromDiscord(
        account.getId(), profile.discordId(), profile.username(),
        profile.globalName(), profile.avatar(), profile.cefrLevel(), profile.roles());
    boolean requiresReactivation = account.getStatus() == AccountStatus.DEACTIVATED;
    if (requiresReactivation) {
      logger.info("Discord login for deactivated account {}", account.getId());
      return DiscordLoginResult.newBuilder()
          .token("")
          .accountId(account.getId().toString())
          .readerAccountId(readerAccountId.toString())
          .requiresReactivation(true)
          .build();
    }
    String token = jwtService.generateToken(account.getId(), account.getPersonId());
    return DiscordLoginResult.newBuilder()
        .token(token)
        .accountId(account.getId().toString())
        .readerAccountId(readerAccountId.toString())
        .requiresReactivation(false)
        .build();
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
        pagination.getSortDir() == null ? null : pagination.getSortDir().name(),
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

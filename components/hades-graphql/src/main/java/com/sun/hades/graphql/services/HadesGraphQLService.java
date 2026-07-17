package com.sun.hades.graphql.services;

import com.sun.base.util.GraphQLSupport;
import com.sun.base.util.PageRequests;
import com.sun.base.util.FilterSpec;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.codegen.types.CommentInput;
import com.sun.hades.codegen.types.DiscordLoginResult;
import com.sun.hades.codegen.types.PageInfo;
import com.sun.hades.codegen.types.PagedReaderComments;
import com.sun.hades.codegen.types.PagedReaderTexts;
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
import com.sun.hades.graphql.mappers.ReaderAccountMapper;
import com.sun.hades.graphql.mappers.ReaderAnnotationMapper;
import com.sun.hades.graphql.mappers.ReaderCommentMapper;
import com.sun.hades.graphql.mappers.ReaderSourceMapper;
import com.sun.hades.graphql.mappers.ReaderTextMapper;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderCommentEntity;
import com.sun.hades.model.ReaderSourceEntity;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.service.DiscordOAuthService;
import com.sun.hades.service.ReaderAccountService;
import com.sun.hades.service.ReaderAnnotationService;
import com.sun.hades.service.ReaderCommentService;
import com.sun.hades.service.ReaderPositionService;
import com.sun.hades.service.ReaderSourceService;
import com.sun.hades.service.ReaderTextService;
import com.sun.hades.service.ReaderVoteService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final DiscordOAuthService discordOAuthService;
  private final AccountService gaiaAccountService;
  private final JwtService jwtService;

  private final ReaderTextMapper textMapper;
  private final ReaderSourceMapper sourceMapper;
  private final ReaderAnnotationMapper annotationMapper;
  private final ReaderCommentMapper commentMapper;
  private final ReaderAccountMapper accountMapper;

  public HadesGraphQLService(ReaderTextService textService, ReaderSourceService sourceService,
      ReaderAnnotationService annotationService, ReaderCommentService commentService,
      ReaderVoteService voteService, ReaderAccountService accountService,
      ReaderPositionService positionService, DiscordOAuthService discordOAuthService,
      AccountService gaiaAccountService, JwtService jwtService, ReaderTextMapper textMapper,
      ReaderSourceMapper sourceMapper, ReaderAnnotationMapper annotationMapper,
      ReaderCommentMapper commentMapper, ReaderAccountMapper accountMapper) {
    this.textService = textService;
    this.sourceService = sourceService;
    this.annotationService = annotationService;
    this.commentService = commentService;
    this.voteService = voteService;
    this.accountService = accountService;
    this.positionService = positionService;
    this.discordOAuthService = discordOAuthService;
    this.gaiaAccountService = gaiaAccountService;
    this.jwtService = jwtService;
    this.textMapper = textMapper;
    this.sourceMapper = sourceMapper;
    this.annotationMapper = annotationMapper;
    this.commentMapper = commentMapper;
    this.accountMapper = accountMapper;
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
  @Transactional(readOnly = true)
  public PagedReaderTexts texts(PaginationInput pagination) {
    Pageable pageable = toPageable(pagination, "level", Sort.Direction.ASC);
    List<FilterSpec> filters = GraphQLSupport.toFilterSpecs(
        pagination != null ? pagination.getFilters() : null,
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
   * Lists annotations for a text, each with its resolved position.
   *
   * @param textId the text id
   * @param includeHidden whether to include hidden annotations
   * @return the annotations
   */
  @Transactional(readOnly = true)
  public List<ReaderAnnotation> annotations(String textId, Boolean includeHidden) {
    UUID id = UUID.fromString(textId);
    List<ReaderAnnotationEntity> entities =
        annotationService.listForText(id, Boolean.TRUE.equals(includeHidden));
    Map<UUID, ReaderPosition> positions =
        positionService.listForText(id).stream()
            .collect(Collectors.toMap(
                p -> p.getId(),
                p -> ReaderPosition.newBuilder()
                    .id(p.getId().toString())
                    .textId(p.getTextId().toString())
                    .startOffset(p.getStartOffset())
                    .endOffset(p.getEndOffset())
                    .build()));
    Map<UUID, RemoteUser> authors = new HashMap<>();
    entities.stream()
        .map(ReaderAnnotationEntity::getCreatedBy)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(gaiaAccountId -> accountService.findByGaiaAccountId(gaiaAccountId)
            .ifPresent(acc -> authors.put(gaiaAccountId, remoteUser(acc.getDiscordId()))));
    Map<UUID, VoteValue> myVotes = voteService.myVotes(
        ReaderVoteTarget.ANNOTATION,
        entities.stream().map(ReaderAnnotationEntity::getId).toList());
    return entities.stream()
        .map(a -> annotationMapper.map(a, positions.get(a.getPositionId()),
            authors.get(a.getCreatedBy()), myVotes.get(a.getId())))
        .toList();
  }

  /**
   * Locates an annotation by id.
   *
   * @param id the annotation id
   * @return the annotation, or null
   */
  @Transactional(readOnly = true)
  public ReaderAnnotation annotation(String id) {
    return annotationService.findById(UUID.fromString(id))
        .map(a -> annotationMapper.map(a, null, null, null))
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
        .filter(c -> Boolean.TRUE.equals(includeHidden) || c.getStatus() == com.sun.hades.model.enums.ReaderStatus.ACTIVE)
        .toList();
    Map<UUID, RemoteUser> authors = new HashMap<>();
    visible.stream()
        .map(ReaderCommentEntity::getCreatedBy)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(gaiaAccountId -> accountService.findByGaiaAccountId(gaiaAccountId)
            .ifPresent(acc -> authors.put(gaiaAccountId, remoteUser(acc.getDiscordId()))));
    Map<UUID, VoteValue> myVotes = voteService.myVotes(
        ReaderVoteTarget.COMMENT,
        visible.stream().map(ReaderCommentEntity::getId).toList());
    List<ReaderComment> items = visible.stream()
        .map(c -> commentMapper.map(c, authors.get(c.getCreatedBy()), myVotes.get(c.getId())))
        .toList();
    return PagedReaderComments.newBuilder().items(items).pageInfo(pageInfo(result)).build();
  }

  /**
   * Builds a DISCORD RemoteUser reference from a Discord id.
   *
   * @param discordId the Discord user id
   * @return the RemoteUser reference
   */
  private RemoteUser remoteUser(String discordId) {
    return RemoteUser.newBuilder()
        .type(RemoteUserType.DISCORD)
        .id(discordId)
        .build();
  }

  /**
   * Returns the current member's reader account.
   *
   * @return the reader account, or null
   */
  @Transactional(readOnly = true)
  public com.sun.hades.codegen.types.ReaderAccount readerAccount() {
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
   * Finds annotations that reference any of the given remote object ids.
   *
   * @param ids the remote object ids
   * @return the matching references
   */
  @Transactional(readOnly = true)
  public List<ReaderObjectReference> locateRemoteObjects(List<String> ids) {
    return annotationService.locateRemoteObjects(ids).stream()
        .map(r -> ReaderObjectReference.newBuilder()
            .id(r.id().toString())
            .ownerType(r.ownerType())
            .ownerId(r.ownerId().toString())
            .build())
        .toList();
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
        input.getParentId() != null ? UUID.fromString(input.getParentId()) : null,
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
   * @param code the authorization code
   * @param state the OAuth state token
   * @return the login result with the JWT
   */
  @Transactional
  public DiscordLoginResult discordLogin(String code, String state) {
    DiscordOAuthService.DiscordProfile profile = discordOAuthService.exchange(code);
    AccountEntity account = gaiaAccountService.upsertProviderAccount(
        "discord", profile.discordId(), profile.username());
    String token = jwtService.generateToken(account.getId(), account.getPersonId());
    UUID readerAccountId = accountService.upsertFromDiscord(
        account.getId(), profile.discordId(), profile.username(),
        profile.globalName(), profile.avatar(), profile.cefrLevel(), profile.roles());
    return DiscordLoginResult.newBuilder()
        .token(token)
        .accountId(account.getId().toString())
        .readerAccountId(readerAccountId.toString())
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
        pagination.getSortDir() != null ? pagination.getSortDir().name() : null,
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
          .id(id != null ? id.toString() : null)
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

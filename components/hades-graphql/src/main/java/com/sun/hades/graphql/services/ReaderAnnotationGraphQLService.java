package com.sun.hades.graphql.services;

import com.sun.hades.codegen.types.PagedReaderAnnotations;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderPosition;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.codegen.types.StandardError;
import com.sun.hades.graphql.mappers.ReaderAnnotationMapper;
import com.sun.hades.graphql.mappers.ReaderPositionMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderPositionEntity;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import com.sun.hades.service.ReaderAccountService;
import com.sun.hades.service.ReaderAnnotationService;
import com.sun.hades.service.ReaderCommentService;
import com.sun.hades.service.ReaderPositionService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for the reader.
 */
@Service
public class ReaderAnnotationGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ReaderAnnotationGraphQLService.class);

  private final ReaderAnnotationService annotationService;
  private final ReaderPositionService positionService;
  private final ReaderAccountService accountService;
  private final ReaderCommentService commentService;
  private final ReaderVoteService voteService;
  private final ReaderAnnotationMapper annotationMapper;
  private final ReaderPositionMapper positionMapper;
  private final RemoteUserMapper remoteUserMapper;

  public ReaderAnnotationGraphQLService(ReaderAnnotationService annotationService,
      ReaderPositionService positionService, ReaderAccountService accountService,
      ReaderCommentService commentService, ReaderVoteService voteService,
      ReaderAnnotationMapper annotationMapper, ReaderPositionMapper positionMapper,
      RemoteUserMapper remoteUserMapper) {
    this.annotationService = annotationService;
    this.positionService = positionService;
    this.accountService = accountService;
    this.commentService = commentService;
    this.voteService = voteService;
    this.annotationMapper = annotationMapper;
    this.positionMapper = positionMapper;
    this.remoteUserMapper = remoteUserMapper;
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
    Pageable pageable = HadesGraphQLSupport.toPageable(pagination, "createdAt", Sort.Direction.DESC);
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
        .pageInfo(HadesGraphQLSupport.pageInfo(page))
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

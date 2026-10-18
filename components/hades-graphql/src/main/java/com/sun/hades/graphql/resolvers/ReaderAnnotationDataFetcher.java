package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.audit.annotation.Audited;
import com.sun.base.ratelimit.RateLimit;
import com.sun.hades.codegen.types.AnnotationInput;
import com.sun.hades.codegen.types.AttachReaderObjectResponse;
import com.sun.hades.codegen.types.CreateAnnotationResponse;
import com.sun.hades.codegen.types.DeleteAnnotationResponse;
import com.sun.hades.codegen.types.EditAnnotationResponse;
import com.sun.hades.codegen.types.PagedReaderAnnotations;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderPosition;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.graphql.mappers.ReaderAnnotationMapper;
import com.sun.hades.graphql.mappers.ReaderPositionMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.graphql.services.HadesGraphQLSupport;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for the reader queries and mutations.
 */
@DgsComponent
public class ReaderAnnotationDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(ReaderAnnotationDataFetcher.class);

  private final ReaderAnnotationService annotationService;
  private final ReaderPositionService positionService;
  private final ReaderAccountService accountService;
  private final ReaderCommentService commentService;
  private final ReaderVoteService voteService;
  private final ReaderAnnotationMapper annotationMapper;
  private final ReaderPositionMapper positionMapper;
  private final RemoteUserMapper remoteUserMapper;

  public ReaderAnnotationDataFetcher(ReaderAnnotationService annotationService,
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
   * Paginated annotations for a text.
   *
   * @param textId the text id
   * @param includeHidden whether to include hidden annotations
   * @param pagination the page request
   * @return the paged annotations
   */
  @Audited(event = "READERANNOTATION_LIST", entity = "ReaderAnnotation")
  @DgsData(parentType = "HadesQueries", field = "annotations")
  @PreAuthorize("@permissions.has('graphql.hades.annotations')")
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
   * @return the annotation
   */
  @Audited(event = "READERANNOTATION_VIEW", entity = "ReaderAnnotation")
  @DgsData(parentType = "HadesQueries", field = "annotation")
  @PreAuthorize("@permissions.has('graphql.hades.annotation')")
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
   * Creates an annotation on a range.
   *
   * @param input the annotation input
   * @return the create-annotation response
   */
  @Audited(event = "READERANNOTATION_CREATE", entity = "ReaderAnnotation", sensitiveFields = {"content"})
  @DgsData(parentType = "HadesMutations", field = "createAnnotation")
  @PreAuthorize("@permissions.has('graphql.hades.createAnnotation')")
  @RateLimit(capacity = 1, refillPerSecond = 0.0667)
  @Transactional
  public CreateAnnotationResponse createAnnotation(AnnotationInput input) {
    UUID id = annotationService.createAnnotation(
        UUID.fromString(input.getTextId()), input.getStartOffset(), input.getEndOffset(), input.getBody());
    logger.info("createAnnotation succeeded for id {}", id);
    ReaderAnnotationEntity annotation = annotationService.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Annotation not found: " + id));
    return CreateAnnotationResponse.newBuilder()
        .message("Annotation created successfully")
        .annotation(annotationMapper.map(annotation, null, null, 0, null))
        .build();
  }

  /**
   * Updates an annotation's body.
   *
   * @param id the annotation id
   * @param body the new body
   * @return the edit-annotation response
   */
  @Audited(event = "READERANNOTATION_UPDATE", entity = "ReaderAnnotation", sensitiveFields = {"body"})
  @DgsData(parentType = "HadesMutations", field = "editAnnotation")
  @PreAuthorize("@permissions.has('graphql.hades.editAnnotation')")
  @Transactional
  public EditAnnotationResponse editAnnotation(String id, String body) {
    UUID annotationId = annotationService.editAnnotation(UUID.fromString(id), body);
    logger.info("editAnnotation succeeded for id {}", annotationId);
    ReaderAnnotationEntity annotation = annotationService.findById(annotationId)
        .orElseThrow(() -> new IllegalArgumentException("Annotation not found: " + annotationId));
    return EditAnnotationResponse.newBuilder()
        .message("Annotation updated successfully")
        .annotation(annotationMapper.map(annotation, null, null, 0, null))
        .build();
  }

  /**
   * Deletes an annotation.
   *
   * @param id the annotation id
   * @return the delete-annotation response
   */
  @Audited(event = "READERANNOTATION_DELETE", entity = "ReaderAnnotation")
  @DgsData(parentType = "HadesMutations", field = "deleteAnnotation")
  @PreAuthorize("@permissions.has('graphql.hades.deleteAnnotation')")
  @Transactional
  public DeleteAnnotationResponse deleteAnnotation(String id) {
    annotationService.deleteAnnotation(UUID.fromString(id));
    logger.info("deleteAnnotation succeeded for id {}", id);
    return DeleteAnnotationResponse.newBuilder()
        .message("Annotation deleted successfully")
        .id(id)
        .build();
  }

  /**
   * Attaches a remote object id to an annotation.
   *
   * @param source the annotation id
   * @param target the remote object id
   * @return the attach-object response
   */
  @Audited(event = "READERANNOTATION_ATTACH", entity = "ReaderAnnotation")
  @DgsData(parentType = "HadesMutations", field = "attachObject")
  @PreAuthorize("@permissions.has('graphql.hades.attachObject')")
  @Transactional
  public AttachReaderObjectResponse attachObject(String source, String target) {
    UUID id = annotationService.attach(UUID.fromString(source), target);
    logger.info("attachObject succeeded for id {}", id);
    return AttachReaderObjectResponse.newBuilder()
        .message("Object attached successfully")
        .id(id.toString())
        .build();
  }
}

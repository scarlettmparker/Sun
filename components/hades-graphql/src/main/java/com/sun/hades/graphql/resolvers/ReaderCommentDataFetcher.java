package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.audit.annotation.Audited;
import com.sun.hades.codegen.types.AddCommentResponse;
import com.sun.hades.codegen.types.CommentInput;
import com.sun.hades.codegen.types.DeleteCommentResponse;
import com.sun.hades.codegen.types.EditCommentResponse;
import com.sun.hades.codegen.types.PagedReaderComments;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.ReaderComment;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.graphql.mappers.ReaderCommentMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.graphql.services.HadesGraphQLSupport;
import com.sun.hades.model.ReaderCommentEntity;
import com.sun.hades.model.enums.ReaderStatus;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import com.sun.hades.service.ReaderAccountService;
import com.sun.hades.service.ReaderCommentService;
import com.sun.hades.service.ReaderVoteService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
public class ReaderCommentDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(ReaderCommentDataFetcher.class);

  private final ReaderCommentService commentService;
  private final ReaderAccountService accountService;
  private final ReaderVoteService voteService;
  private final ReaderCommentMapper commentMapper;
  private final RemoteUserMapper remoteUserMapper;

  public ReaderCommentDataFetcher(ReaderCommentService commentService,
      ReaderAccountService accountService, ReaderVoteService voteService,
      ReaderCommentMapper commentMapper, RemoteUserMapper remoteUserMapper) {
    this.commentService = commentService;
    this.accountService = accountService;
    this.voteService = voteService;
    this.commentMapper = commentMapper;
    this.remoteUserMapper = remoteUserMapper;
  }

  /**
   * Lists comments for an annotation.
   *
   * @param annotationId the annotation id
   * @param includeHidden whether to include hidden comments
   * @param pagination the pagination input
   * @return a page of comments
   */
  @Audited(event = "READERCOMMENT_LIST", entity = "ReaderComment")
  @DgsData(parentType = "HadesQueries", field = "comments")
  @PreAuthorize("@permissions.has('graphql.hades.comments')")
  @Transactional(readOnly = true)
  public PagedReaderComments comments(String annotationId, Boolean includeHidden, PaginationInput pagination) {
    Pageable pageable = HadesGraphQLSupport.toPageable(pagination, "createdAt", Sort.Direction.ASC);
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
    return PagedReaderComments.newBuilder().items(items).pageInfo(HadesGraphQLSupport.pageInfo(result)).build();
  }

  /**
   * Adds a comment.
   *
   * @param input the comment input
   * @return the add-comment response
   */
  @Audited(event = "READERCOMMENT_CREATE", entity = "ReaderComment", sensitiveFields = {"body"})
  @DgsData(parentType = "HadesMutations", field = "addComment")
  @PreAuthorize("@permissions.has('graphql.hades.addComment')")
  @Transactional
  public AddCommentResponse addComment(CommentInput input) {
    UUID id = commentService.addComment(
        UUID.fromString(input.getAnnotationId()),
        input.getParentId() == null ? null : UUID.fromString(input.getParentId()),
        input.getBody());
    logger.info("addComment succeeded for id {}", id);
    ReaderCommentEntity comment = commentService.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + id));
    return AddCommentResponse.newBuilder()
        .message("Comment added successfully")
        .comment(commentMapper.map(comment, null, null))
        .build();
  }

  /**
   * Updates a comment's body.
   *
   * @param id the comment id
   * @param body the new body
   * @return the edit-comment response
   */
  @Audited(event = "READERCOMMENT_UPDATE", entity = "ReaderComment", sensitiveFields = {"body"})
  @DgsData(parentType = "HadesMutations", field = "editComment")
  @PreAuthorize("@permissions.has('graphql.hades.editComment')")
  @Transactional
  public EditCommentResponse editComment(String id, String body) {
    UUID commentId = commentService.editComment(UUID.fromString(id), body);
    logger.info("editComment succeeded for id {}", commentId);
    ReaderCommentEntity comment = commentService.findById(commentId)
        .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
    return EditCommentResponse.newBuilder()
        .message("Comment updated successfully")
        .comment(commentMapper.map(comment, null, null))
        .build();
  }

  /**
   * Deletes a comment.
   *
   * @param id the comment id
   * @return the delete-comment response
   */
  @Audited(event = "READERCOMMENT_DELETE", entity = "ReaderComment")
  @DgsData(parentType = "HadesMutations", field = "deleteComment")
  @PreAuthorize("@permissions.has('graphql.hades.deleteComment')")
  @Transactional
  public DeleteCommentResponse deleteComment(String id) {
    commentService.deleteComment(UUID.fromString(id));
    logger.info("deleteComment succeeded for id {}", id);
    return DeleteCommentResponse.newBuilder()
        .message("Comment deleted successfully")
        .id(id)
        .build();
  }
}

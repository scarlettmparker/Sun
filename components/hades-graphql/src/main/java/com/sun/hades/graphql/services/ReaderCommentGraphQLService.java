package com.sun.hades.graphql.services;

import com.sun.base.error.MutationException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for the reader.
 */
@Service
public class ReaderCommentGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ReaderCommentGraphQLService.class);

  private final ReaderCommentService commentService;
  private final ReaderAccountService accountService;
  private final ReaderVoteService voteService;
  private final ReaderCommentMapper commentMapper;
  private final RemoteUserMapper remoteUserMapper;

  public ReaderCommentGraphQLService(ReaderCommentService commentService,
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
  @Transactional(readOnly = true)
  public PagedReaderComments comments(
      String annotationId, Boolean includeHidden, PaginationInput pagination) {
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
   * Adds a comment to an annotation.
   *
   * @param input the comment input
   * @return the add-comment response
   */
  @Transactional
  public AddCommentResponse addComment(CommentInput input) {
    try {
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
    } catch (Exception e) {
      logger.error("addComment failed", e);
      throw new MutationException("addComment failed: " + e.getMessage(), e);
    }
  }

  /**
   * Updates a comment's body.
   *
   * @param id the comment id
   * @param body the new body
   * @return the edit-comment response
   */
  @Transactional
  public EditCommentResponse editComment(String id, String body) {
    try {
      UUID commentId = commentService.editComment(UUID.fromString(id), body);
      logger.info("editComment succeeded for id {}", commentId);
      ReaderCommentEntity comment = commentService.findById(commentId)
          .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
      return EditCommentResponse.newBuilder()
          .message("Comment updated successfully")
          .comment(commentMapper.map(comment, null, null))
          .build();
    } catch (Exception e) {
      logger.error("editComment failed", e);
      throw new MutationException("editComment failed: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes a comment.
   *
   * @param id the comment id
   * @return the delete-comment response
   */
  @Transactional
  public DeleteCommentResponse deleteComment(String id) {
    try {
      commentService.deleteComment(UUID.fromString(id));
      logger.info("deleteComment succeeded for id {}", id);
      return DeleteCommentResponse.newBuilder()
          .message("Comment deleted successfully")
          .id(id)
          .build();
    } catch (Exception e) {
      logger.error("deleteComment failed", e);
      throw new MutationException("deleteComment failed: " + e.getMessage(), e);
    }
  }
}

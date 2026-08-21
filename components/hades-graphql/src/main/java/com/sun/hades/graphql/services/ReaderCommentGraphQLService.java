package com.sun.hades.graphql.services;

import com.sun.hades.codegen.types.CommentInput;
import com.sun.hades.codegen.types.PagedReaderComments;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.QuerySuccess;
import com.sun.hades.codegen.types.ReaderComment;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.codegen.types.StandardError;
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
import java.util.function.Supplier;
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

package com.sun.hades.graphql.services;

import com.sun.base.error.MutationException;
import com.sun.hades.codegen.types.VoteInput;
import com.sun.hades.codegen.types.VoteReaderResponse;
import com.sun.hades.graphql.mappers.ReaderAnnotationMapper;
import com.sun.hades.graphql.mappers.ReaderCommentMapper;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderCommentEntity;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import com.sun.hades.service.ReaderAnnotationService;
import com.sun.hades.service.ReaderCommentService;
import com.sun.hades.service.ReaderVoteService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for the reader.
 */
@Service
public class ReaderVoteGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ReaderVoteGraphQLService.class);

  private final ReaderVoteService voteService;
  private final ReaderAnnotationService annotationService;
  private final ReaderCommentService commentService;
  private final ReaderAnnotationMapper annotationMapper;
  private final ReaderCommentMapper commentMapper;

  public ReaderVoteGraphQLService(ReaderVoteService voteService,
      ReaderAnnotationService annotationService, ReaderCommentService commentService,
      ReaderAnnotationMapper annotationMapper, ReaderCommentMapper commentMapper) {
    this.voteService = voteService;
    this.annotationService = annotationService;
    this.commentService = commentService;
    this.annotationMapper = annotationMapper;
    this.commentMapper = commentMapper;
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
   * Casts, toggles, or flips a vote.
   *
   * @param input the vote input
   * @return the vote response with the affected target
   */
  @Transactional
  public VoteReaderResponse vote(VoteInput input) {
    try {
      UUID id = voteService.vote(
          input.getTargetType(),
          UUID.fromString(input.getTargetId()),
          input.getValue());
      logger.info("vote succeeded for id {}", id);
      return response("Vote recorded successfully", input.getTargetType(), id);
    } catch (Exception e) {
      logger.error("vote failed", e);
      throw new MutationException("vote failed: " + e.getMessage(), e);
    }
  }

  /**
   * Removes the caller's vote.
   *
   * @param targetType the target type
   * @param targetId the target id
   * @return the vote response with the affected target
   */
  @Transactional
  public VoteReaderResponse removeVote(ReaderVoteTarget targetType, String targetId) {
    try {
      UUID id = voteService.removeVote(targetType, UUID.fromString(targetId));
      logger.info("removeVote succeeded for id {}", id);
      return response("Vote removed successfully", targetType, id);
    } catch (Exception e) {
      logger.error("removeVote failed", e);
      throw new MutationException("removeVote failed: " + e.getMessage(), e);
    }
  }

  /**
   * Builds a vote response for the affected annotation or comment.
   *
   * @param message the success message
   * @param targetType whether the target is an annotation or comment
   * @param id the target id
   * @return the vote response
   */
  private VoteReaderResponse response(String message, ReaderVoteTarget targetType, UUID id) {
    VoteReaderResponse.Builder builder = VoteReaderResponse.newBuilder().message(message);
    if (targetType == ReaderVoteTarget.ANNOTATION) {
      ReaderAnnotationEntity annotation = annotationService.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Annotation not found: " + id));
      return builder.annotation(annotationMapper.map(annotation, null, null, 0, null)).build();
    }
    ReaderCommentEntity comment = commentService.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + id));
    return builder.comment(commentMapper.map(comment, null, null)).build();
  }
}

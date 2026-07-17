package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderComment;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.model.ReaderCommentEntity;
import com.sun.hades.model.enums.VoteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader comment entities.
 */
@Component
public class ReaderCommentMapper {

  private static final Logger logger = LoggerFactory.getLogger(ReaderCommentMapper.class);

  /**
   * Maps a comment entity to the GraphQL ReaderComment type, with its author
   * reference and the caller's vote resolved by the caller.
   *
   * @param entity the comment entity
   * @param author the resolved author reference, or null
   * @param myVote the caller's vote on this comment, or null
   * @return the GraphQL ReaderComment
   */
  public ReaderComment map(ReaderCommentEntity entity, RemoteUser author, VoteValue myVote) {
    logger.debug("Mapping reader comment {}", entity.getId());
    ReaderComment comment = ReaderComment.newBuilder()
        .id(entity.getId().toString())
        .annotationId(entity.getAnnotationId().toString())
        .parentId(entity.getParentId() != null ? entity.getParentId().toString() : null)
        .body(entity.getBody())
        .status(entity.getStatus())
        .upvotes(entity.getUpvotes())
        .downvotes(entity.getDownvotes())
        .netScore(entity.getUpvotes() - entity.getDownvotes())
        .author(author)
        .myVote(myVote)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
    logger.debug("Mapped reader comment with id {}", comment.getId());
    return comment;
  }
}

package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderComment;
import com.sun.hades.model.ReaderCommentEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader comment entities.
 */
@Component
public class ReaderCommentMapper {

  /**
   * Maps a comment entity to the GraphQL ReaderComment type.
   *
   * @param entity the comment entity
   * @return the GraphQL ReaderComment
   */
  public ReaderComment map(ReaderCommentEntity entity) {
    return ReaderComment.newBuilder()
        .id(entity.getId().toString())
        .annotationId(entity.getAnnotationId().toString())
        .parentId(entity.getParentId() != null ? entity.getParentId().toString() : null)
        .body(entity.getBody())
        .status(entity.getStatus())
        .upvotes(entity.getUpvotes())
        .downvotes(entity.getDownvotes())
        .netScore(entity.getUpvotes() - entity.getDownvotes())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }
}

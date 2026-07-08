package com.sun.icarus.graphql.mappers;

import com.sun.icarus.codegen.types.ForumPost;
import com.sun.icarus.model.ForumPostEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for forum post entities.
 */
@Component
public class ForumPostMapper {

  /**
   * Maps a post entity to the GraphQL ForumPost type.
   *
   * @param entity the post entity
   * @return the GraphQL ForumPost
   */
  public ForumPost map(ForumPostEntity entity) {
    return ForumPost.newBuilder()
        .id(entity.getId().toString())
        .threadId(entity.getThreadId().toString())
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

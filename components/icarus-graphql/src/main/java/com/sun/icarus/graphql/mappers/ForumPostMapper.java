package com.sun.icarus.graphql.mappers;

import com.sun.icarus.codegen.types.ForumPost;
import com.sun.icarus.model.ForumPostEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for forum post entities.
 */
@Component
public class ForumPostMapper {

  private static final Logger logger = LoggerFactory.getLogger(ForumPostMapper.class);

  /**
   * Maps a post entity to the GraphQL ForumPost type.
   *
   * @param entity the post entity
   * @return the GraphQL ForumPost
   */
  public ForumPost map(ForumPostEntity entity) {
    logger.debug("Mapping forum post {}", entity.getId());
    ForumPost post = ForumPost.newBuilder()
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
    logger.debug("Mapped forum post with id {}", post.getId());
    return post;
  }
}

package com.sun.icarus.graphql.mappers;

import com.sun.icarus.codegen.types.ForumPost;
import com.sun.icarus.codegen.types.RemoteUser;
import com.sun.icarus.model.ForumPostEntity;
import com.sun.icarus.model.enums.VoteValue;
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
   * Maps a post entity to the GraphQL ForumPost type, with its author reference
   * and the caller's vote resolved by the caller.
   *
   * @param entity the post entity
   * @param author the resolved author reference, or null
   * @param myVote the caller's vote on this post, or null
   * @return the GraphQL ForumPost
   */
  public ForumPost map(ForumPostEntity entity, RemoteUser author, VoteValue myVote) {
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
        .author(author)
        .myVote(myVote)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
    logger.debug("Mapped forum post with id {}", post.getId());
    return post;
  }
}

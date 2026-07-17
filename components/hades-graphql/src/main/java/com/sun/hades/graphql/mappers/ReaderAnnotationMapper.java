package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderPosition;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.enums.VoteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader annotation entities.
 */
@Component
public class ReaderAnnotationMapper {

  private static final Logger logger = LoggerFactory.getLogger(ReaderAnnotationMapper.class);

  /**
   * Maps an annotation entity to the GraphQL ReaderAnnotation type, with its
   * position, author, reply count, and the caller's vote resolved by the caller.
   *
   * @param entity the annotation entity
   * @param position the resolved position, or null
   * @param author the resolved author reference, or null
   * @param replyCount the number of active replies
   * @param myVote the caller's vote on this annotation, or null
   * @return the GraphQL ReaderAnnotation
   */
  public ReaderAnnotation map(ReaderAnnotationEntity entity, ReaderPosition position,
      RemoteUser author, int replyCount, VoteValue myVote) {
    logger.debug("Mapping annotation {}", entity.getId());
    ReaderAnnotation annotation = ReaderAnnotation.newBuilder()
        .id(entity.getId().toString())
        .positionId(entity.getPositionId().toString())
        .position(position)
        .body(entity.getBody())
        .status(entity.getStatus())
        .upvotes(entity.getUpvotes())
        .downvotes(entity.getDownvotes())
        .netScore(entity.getUpvotes() - entity.getDownvotes())
        .replyCount(replyCount)
        .remoteObject(entity.getRemoteObject())
        .author(author)
        .myVote(myVote)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
    logger.debug("Mapped annotation with id {}", annotation.getId());
    return annotation;
  }
}

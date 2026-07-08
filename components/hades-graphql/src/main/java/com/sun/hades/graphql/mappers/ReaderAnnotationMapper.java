package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderPosition;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderPositionEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader annotation entities.
 */
@Component
public class ReaderAnnotationMapper {

  /**
   * Maps an annotation entity to the GraphQL ReaderAnnotation type, with its
   * position resolved by the caller.
   *
   * @param entity the annotation entity
   * @param position the resolved position, or null
   * @return the GraphQL ReaderAnnotation
   */
  public ReaderAnnotation map(ReaderAnnotationEntity entity, ReaderPosition position) {
    return ReaderAnnotation.newBuilder()
        .id(entity.getId().toString())
        .positionId(entity.getPositionId().toString())
        .position(position)
        .body(entity.getBody())
        .status(entity.getStatus())
        .upvotes(entity.getUpvotes())
        .downvotes(entity.getDownvotes())
        .netScore(entity.getUpvotes() - entity.getDownvotes())
        .remoteObject(entity.getRemoteObject())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }
}

package com.sun.icarus.graphql.mappers;

import com.sun.icarus.codegen.types.ForumThread;
import com.sun.icarus.model.ForumThreadEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for forum thread entities.
 */
@Component
public class ForumThreadMapper {

  /**
   * Maps a thread entity to the GraphQL ForumThread type.
   *
   * @param entity the thread entity
   * @return the GraphQL ForumThread
   */
  public ForumThread map(ForumThreadEntity entity) {
    return ForumThread.newBuilder()
        .id(entity.getId().toString())
        .title(entity.getTitle())
        .status(entity.getStatus())
        .remoteObject(entity.getRemoteObject())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }
}

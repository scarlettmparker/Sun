package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.PrivateNote;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.model.PrivateNoteEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for private note entities.
 */
@Component
public class PrivateNoteMapper {

  /**
   * Maps a private note entity to the GraphQL PrivateNote type.
   *
   * @param entity the note entity
   * @param author the resolved author reference, or null
   * @return the GraphQL PrivateNote
   */
  public PrivateNote map(PrivateNoteEntity entity, RemoteUser author) {
    return PrivateNote.newBuilder()
        .id(entity.getId().toString())
        .textId(entity.getTextId().toString())
        .startOffset(entity.getStartOffset())
        .endOffset(entity.getEndOffset())
        .body(entity.getBody())
        .visibility(
            com.sun.hades.codegen.types.PrivateNoteVisibility.valueOf(
                entity.getVisibility().name()))
        .remoteObject(entity.getRemoteObject())
        .author(author)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }
}

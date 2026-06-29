package com.sun.echo.graphql.mappers;

import com.sun.echo.codegen.types.ChecklistDetail;
import com.sun.echo.model.AbstractDetailEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting checklist detail entities to GraphQL types.
 */
@Component
public class ChecklistDetailMapper {

  /**
   * Maps a detail entity to the GraphQL ChecklistDetail type.
   *
   * @param entity the detail entity
   * @return the GraphQL ChecklistDetail
   */
  public ChecklistDetail map(AbstractDetailEntity entity) {
    return ChecklistDetail.newBuilder()
        .ownerId(entity.getOwnerId().toString())
        .description(entity.getDescription())
        .remoteObject(entity.getRemoteObject())
        .build();
  }
}

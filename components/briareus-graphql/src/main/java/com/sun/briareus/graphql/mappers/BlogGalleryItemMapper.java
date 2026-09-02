package com.sun.briareus.graphql.mappers;

import com.sun.briareus.codegen.types.BlogGalleryItem;
import com.sun.cerberus.model.GalleryItemEntity;
import org.springframework.stereotype.Component;

/**
 * Maps gallery entities.
 */
@Component
public class BlogGalleryItemMapper {

  /**
   * Maps a domain gallery item to GraphQL.
   *
   * @param entity the domain entity
   * @return the mapped GraphQL type
   */
  public BlogGalleryItem map(GalleryItemEntity entity) {
    return BlogGalleryItem.newBuilder()
        .id(entity.getId().toString())
        .title(entity.getTitle())
        .imagePath(entity.getImagePath())
        .remoteObject(entity.getRemoteObject())
        .build();
  }
}

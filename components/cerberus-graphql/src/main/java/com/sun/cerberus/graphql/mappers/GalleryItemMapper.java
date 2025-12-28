package com.sun.cerberus.graphql.mappers;

import com.sun.cerberus.codegen.types.GalleryItem;
import com.sun.cerberus.codegen.types.GalleryItemInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.sun.cerberus.model.GalleryItemEntity;

/**
 * Mapper for converting domain GalleryItem entities to GraphQL GalleryItem
 * types.
 */
@Component
public class GalleryItemMapper {

  private static final Logger logger = LoggerFactory.getLogger(GalleryItemMapper.class);

  /**
   * Maps a domain GalleryItemEntity to a GraphQL GalleryItem type.
   *
   * @param galleryItemEntity the domain GalleryItemEntity to map
   * @return the mapped GraphQL GalleryItem type.
   */
  public GalleryItem map(GalleryItemEntity galleryItemEntity) {
    logger.debug("Mapping gallery item {}", galleryItemEntity.getTitle());

    GalleryItem galleryItem = GalleryItem.newBuilder()
        .id(galleryItemEntity.getId().toString())
        .title(galleryItemEntity.getTitle())
        .description(galleryItemEntity.getDescription())
        .content(galleryItemEntity.getContent())
        .imagePath(galleryItemEntity.getImagePath())
        .foreignObject(galleryItemEntity.getForeignObject())
        .createdAt(galleryItemEntity.getCreatedAt())
        .updatedAt(galleryItemEntity.getLastUpdatedAt())
        .build();

    logger.debug("Mapped gallery item {} with id {}", galleryItemEntity.getTitle(), galleryItemEntity.getId());
    return galleryItem;
  }

  /**
   * Maps a GraphQL GalleryItemInput to a domain GalleryItemEntity.
   *
   * @param input the GraphQL GalleryItemInput to map
   * @return the mapped domain GalleryItemEntity
   */
  public GalleryItemEntity mapInput(GalleryItemInput input) {
    logger.debug("Mapping input for gallery item with title: {}", input.getTitle());

    GalleryItemEntity galleryItemEntity = new GalleryItemEntity();
    galleryItemEntity.setTitle(input.getTitle());
    galleryItemEntity.setDescription(input.getDescription());
    galleryItemEntity.setContent(input.getContent());
    galleryItemEntity.setImagePath(input.getImagePath());
    galleryItemEntity.setForeignObject(input.getForeignObject());

    logger.debug("Mapped input to gallery item entity with title: {}", input.getTitle());
    return galleryItemEntity;
  }

}
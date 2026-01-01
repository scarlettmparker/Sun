package com.sun.cerberus.graphql.services;

import com.sun.cerberus.service.CerberusService;
import com.sun.cerberus.graphql.mappers.GalleryItemMapper;
import com.sun.cerberus.codegen.types.GalleryItem;
import com.sun.cerberus.codegen.types.GalleryItemInput;
import com.sun.cerberus.codegen.types.QueryResult;
import com.sun.cerberus.codegen.types.QuerySuccess;
import com.sun.cerberus.codegen.types.StandardError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import com.sun.cerberus.model.GalleryItemEntity;

/**
 * Service for handling GraphQL-specific business logic for the Gallery.
 * This service acts as an intermediary between the GraphQL layer and the domain
 * services.
 */
@Service
public class GalleryGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(GalleryGraphQLService.class);

  @Autowired
  private CerberusService cerberusService;

  @Autowired
  private GalleryItemMapper galleryItemMapper;

  /**
   * Retrieves all gallery items.
   *
   * @return a list of GraphQL GalleryItem objects
   */
  @Transactional("cerberusTransactionManager")
  public List<GalleryItem> list() {
    logger.info("Retrieving gallery items");

    List<GalleryItemEntity> galleryItemEntities = cerberusService.list();
    List<GalleryItem> galleryItems = galleryItemEntities.stream()
        .map(galleryItemEntity -> galleryItemMapper.map(galleryItemEntity))
        .collect(Collectors.toList());

    logger.info("Retrieved {} gallery items", galleryItems.size());
    return galleryItems;
  }

  /**
   * Retrieves a specific gallery item with its information by ID.
   *
   * @param id the gallery item ID as string
   * @return the GraphQL GalleryItem object
   */
  @Transactional("cerberusTransactionManager")
  public GalleryItem locate(String id) {
    logger.info("Retrieving gallery item by ID: {}", id);

    GalleryItemEntity galleryItemEntity = cerberusService.locate(java.util.UUID.fromString(id))
        .orElseThrow(() -> new RuntimeException("Gallery item not found with id: " + id));

    GalleryItem galleryItem = galleryItemMapper.map(galleryItemEntity);

    logger.info("Retrieved gallery item {} with id {}", galleryItem.getTitle(), galleryItem.getId());
    return galleryItem;
  }

  /**
   * Retrieves gallery items that have any of the specified foreign object IDs.
   *
   * @param ids the list of foreign object IDs to search for
   * @return a list of GraphQL GalleryItem objects
   */
  @Transactional("cerberusTransactionManager")
  public List<GalleryItem> listByForeignObject(List<String> ids) {
    logger.info("Retrieving gallery items by foreign object ids: {}", ids);

    List<GalleryItemEntity> galleryItemEntities = cerberusService.listByForeignObject(ids);
    List<GalleryItem> galleryItems = galleryItemEntities.stream()
        .map(galleryItemEntity -> galleryItemMapper.map(galleryItemEntity))
        .collect(Collectors.toList());

    logger.info("Retrieved {} gallery items matching foreign object ids", galleryItems.size());
    return galleryItems;
  }

  /**
   * Creates a new gallery item.
   *
   * @param input the input data for the gallery item
   * @return QueryResult indicating success or error
   */
  @Transactional("cerberusTransactionManager")
  public QueryResult create(GalleryItemInput input) {
    logger.info("Creating gallery item with title: {}", input.getTitle());

    try {
      GalleryItemEntity galleryItemEntity = galleryItemMapper.mapInput(input);
      GalleryItemEntity savedEntity = cerberusService.save(galleryItemEntity);

      logger.info("Successfully created gallery item with id: {}", savedEntity.getId());
      return QuerySuccess.newBuilder()
          .message("Gallery item created successfully")
          .id(savedEntity.getId().toString())
          .build();
    } catch (Exception e) {
      logger.error("Failed to create gallery item with title: {}", input.getTitle(), e);
      return StandardError.newBuilder()
          .message("Failed to create gallery item: " + e.getMessage())
          .build();
    }
  }
}
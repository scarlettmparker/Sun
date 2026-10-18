package com.sun.cerberus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.error.MutationException;
import com.sun.cerberus.codegen.types.CreateGalleryItemResponse;
import com.sun.cerberus.codegen.types.GalleryItem;
import com.sun.cerberus.codegen.types.GalleryItemInput;
import com.sun.cerberus.codegen.types.GalleryMutations;
import com.sun.cerberus.codegen.types.GalleryQueries;
import com.sun.cerberus.graphql.mappers.GalleryItemMapper;
import com.sun.cerberus.model.GalleryItemEntity;
import com.sun.cerberus.service.CerberusService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@DgsComponent
public class GalleryDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(GalleryDataFetcher.class);

  private final CerberusService cerberusService;
  private final GalleryItemMapper galleryItemMapper;

  public GalleryDataFetcher(CerberusService cerberusService, GalleryItemMapper galleryItemMapper) {
    this.cerberusService = cerberusService;
    this.galleryItemMapper = galleryItemMapper;
  }

  /**
   * Provides the gallery queries object.
   *
   * @return a new GalleryQueries instance
   */
  @DgsData(parentType = "Query", field = "galleryQueries")
  public GalleryQueries getGalleryQueries() {
    return GalleryQueries.newBuilder().build();
  }

  /**
   * Retrieves all gallery items.
   *
   * @return a list of GalleryItem objects
   */
  @DgsData(parentType = "GalleryQueries", field = "list")
  @PreAuthorize("@permissions.has('graphql.cerberus.list')")
  @Transactional
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
   * Retrieves a specific gallery item by ID.
   *
   * @param id the gallery item ID
   * @return the GalleryItem object
   */
  @DgsData(parentType = "GalleryQueries", field = "locate")
  @PreAuthorize("@permissions.has('graphql.cerberus.locate')")
  @Transactional
  public GalleryItem locate(String id) {
    logger.info("Retrieving gallery item by ID: {}", id);

    GalleryItemEntity galleryItemEntity = cerberusService.locate(UUID.fromString(id))
        .orElseThrow(() -> new RuntimeException("Gallery item not found with id: " + id));

    GalleryItem galleryItem = galleryItemMapper.map(galleryItemEntity);

    logger.info("Retrieved gallery item {} with id {}", galleryItem.getTitle(), galleryItem.getId());
    return galleryItem;
  }

  /**
   * Retrieves gallery items that have any of the specified foreign object IDs.
   *
   * @param ids the list of foreign object IDs to search for
   * @return a list of GalleryItem objects
   */
  @DgsData(parentType = "GalleryQueries", field = "listByRemoteObjects")
  @PreAuthorize("@permissions.has('graphql.cerberus.listByRemoteObjects')")
  @Transactional
  public List<GalleryItem> listByRemoteObjects(List<String> ids) {
    logger.info("Retrieving gallery items by foreign object ids: {}", ids);

    List<GalleryItemEntity> galleryItemEntities = cerberusService.listByRemoteObjects(ids);
    List<GalleryItem> galleryItems = galleryItemEntities.stream()
        .map(galleryItemEntity -> galleryItemMapper.map(galleryItemEntity))
        .collect(Collectors.toList());

    logger.info("Retrieved {} gallery items matching foreign object ids", galleryItems.size());
    return galleryItems;
  }

  /**
   * Retrieves gallery items by their IDs (batch lookup).
   *
   * @param ids the gallery item IDs
   * @return a list of GalleryItem objects
   */
  @DgsData(parentType = "GalleryQueries", field = "locateGalleryItems")
  @PreAuthorize("@permissions.has('graphql.cerberus.locateGalleryItems')")
  @Transactional
  public List<GalleryItem> locateGalleryItems(List<String> ids) {
    logger.info("Retrieving gallery items by ids: {}", ids);

    List<UUID> uuids = ids.stream()
        .map(UUID::fromString)
        .collect(Collectors.toList());
    List<GalleryItemEntity> galleryItemEntities = cerberusService.locateByIds(uuids);
    return galleryItemEntities.stream()
        .map(galleryItemMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Provides the gallery mutations object.
   *
   * @return a new GalleryMutations instance
   */
  @DgsData(parentType = "Mutation", field = "galleryMutations")
  public GalleryMutations getGalleryMutations() {
    return GalleryMutations.newBuilder().build();
  }

  /**
   * Creates a new gallery item.
   *
   * @param input the input data for the gallery item
   * @return the created gallery item
   */
  @DgsData(parentType = "GalleryMutations", field = "create")
  @PreAuthorize("@permissions.has('graphql.cerberus.create')")
  @Transactional
  public CreateGalleryItemResponse create(GalleryItemInput input) {
    logger.info("Creating gallery item with title: {}", input.getTitle());

    try {
      GalleryItemEntity galleryItemEntity = galleryItemMapper.mapInput(input);
      GalleryItemEntity savedEntity = cerberusService.save(galleryItemEntity);

      logger.info("Successfully created gallery item with id: {}", savedEntity.getId());
      return CreateGalleryItemResponse.newBuilder()
          .message("Gallery item created successfully")
          .item(galleryItemMapper.map(savedEntity))
          .build();
    } catch (Exception e) {
      logger.error("Failed to create gallery item with title: {}", input.getTitle(), e);
      throw new MutationException("Failed to create gallery item: " + e.getMessage(), e);
    }
  }
}

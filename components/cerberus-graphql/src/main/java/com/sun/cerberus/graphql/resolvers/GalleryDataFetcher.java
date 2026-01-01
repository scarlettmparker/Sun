package com.sun.cerberus.graphql.resolvers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.sun.cerberus.graphql.services.GalleryGraphQLService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.cerberus.codegen.types.GalleryItem;
import com.sun.cerberus.codegen.types.GalleryQueries;
import com.sun.cerberus.codegen.types.GalleryMutations;
import com.sun.cerberus.codegen.types.GalleryItemInput;
import com.sun.cerberus.codegen.types.QueryResult;

@DgsComponent
public class GalleryDataFetcher {

  @Autowired
  private GalleryGraphQLService galleryGraphQLService;

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
  public List<GalleryItem> list() {
    return galleryGraphQLService.list();
  }

  /**
   * Retrieves a specific gallery item by ID.
   *
   * @param id the gallery item ID
   * @return the GalleryItem object
   */
  @DgsData(parentType = "GalleryQueries", field = "locate")
  public GalleryItem locate(String id) {
    return galleryGraphQLService.locate(id);
  }

  /**
   * Retrieves gallery items that have any of the specified foreign object IDs.
   *
   * @param ids the list of foreign object IDs to search for
   * @return a list of GalleryItem objects
   */
  @DgsData(parentType = "GalleryQueries", field = "listByForeignObject")
  public List<GalleryItem> listByForeignObject(List<String> ids) {
    return galleryGraphQLService.listByForeignObject(ids);
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
   * @return QueryResult indicating success or error
   */
  @DgsData(parentType = "GalleryMutations", field = "create")
  public QueryResult create(GalleryItemInput input) {
    return galleryGraphQLService.create(input);
  }
}
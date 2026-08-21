package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.ratelimit.RateLimit;
import com.sun.hades.codegen.types.AnnotationInput;
import com.sun.hades.codegen.types.PagedReaderAnnotations;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.graphql.services.ReaderAnnotationGraphQLService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for the reader queries and mutations.
 */
@DgsComponent
public class ReaderAnnotationDataFetcher {

  private final ReaderAnnotationGraphQLService readerAnnotationGraphQLService;

  public ReaderAnnotationDataFetcher(ReaderAnnotationGraphQLService readerAnnotationGraphQLService) {
    this.readerAnnotationGraphQLService = readerAnnotationGraphQLService;
  }

  /**
   * Paginated annotations for a text.
   *
   * @param textId the text id
   * @param includeHidden whether to include hidden annotations
   * @param pagination the page request
   * @return the paged annotations
   */
  @DgsData(parentType = "HadesQueries", field = "annotations")
  @PreAuthorize("@permissions.has('graphql.hades.annotations')")
  public PagedReaderAnnotations annotations(
      String textId, Boolean includeHidden, PaginationInput pagination) {
    return readerAnnotationGraphQLService.annotations(textId, includeHidden, pagination);
  }

  /**
   * Locates an annotation by id.
   *
   * @param id the annotation id
   * @return the annotation
   */
  @DgsData(parentType = "HadesQueries", field = "annotation")
  @PreAuthorize("@permissions.has('graphql.hades.annotation')")
  public ReaderAnnotation annotation(String id) {
    return readerAnnotationGraphQLService.annotation(id);
  }

  /**
   * Creates an annotation on a range.
   *
   * @param input the annotation input
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "createAnnotation")
  @PreAuthorize("@permissions.has('graphql.hades.createAnnotation')")
  @RateLimit(capacity = 1, refillPerSecond = 0.0667)
  public QueryResult createAnnotation(AnnotationInput input) {
    return readerAnnotationGraphQLService.createAnnotation(
        input.getTextId(), input.getStartOffset(), input.getEndOffset(), input.getBody());
  }

  /**
   * Updates an annotation's body.
   *
   * @param id the annotation id
   * @param body the new body
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "editAnnotation")
  @PreAuthorize("@permissions.has('graphql.hades.editAnnotation')")
  public QueryResult editAnnotation(String id, String body) {
    return readerAnnotationGraphQLService.editAnnotation(id, body);
  }

  /**
   * Deletes an annotation.
   *
   * @param id the annotation id
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "deleteAnnotation")
  @PreAuthorize("@permissions.has('graphql.hades.deleteAnnotation')")
  public QueryResult deleteAnnotation(String id) {
    return readerAnnotationGraphQLService.deleteAnnotation(id);
  }

  /**
   * Attaches a remote object id to an annotation.
   *
   * @param source the annotation id
   * @param target the remote object id
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "attachObject")
  @PreAuthorize("@permissions.has('graphql.hades.attachObject')")
  public QueryResult attachObject(String source, String target) {
    return readerAnnotationGraphQLService.attachObject(source, target);
  }
}

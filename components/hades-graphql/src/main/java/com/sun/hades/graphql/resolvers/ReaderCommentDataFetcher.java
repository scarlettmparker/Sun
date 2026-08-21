package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.hades.codegen.types.CommentInput;
import com.sun.hades.codegen.types.PagedReaderComments;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.graphql.services.ReaderCommentGraphQLService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for the reader queries and mutations.
 */
@DgsComponent
public class ReaderCommentDataFetcher {

  private final ReaderCommentGraphQLService readerCommentGraphQLService;

  public ReaderCommentDataFetcher(ReaderCommentGraphQLService readerCommentGraphQLService) {
    this.readerCommentGraphQLService = readerCommentGraphQLService;
  }

  /**
   * Lists comments for an annotation.
   *
   * @param annotationId the annotation id
   * @param includeHidden whether to include hidden comments
   * @param pagination the pagination input
   * @return a page of comments
   */
  @DgsData(parentType = "HadesQueries", field = "comments")
  @PreAuthorize("@permissions.has('graphql.hades.comments')")
  public PagedReaderComments comments(String annotationId, Boolean includeHidden, PaginationInput pagination) {
    return readerCommentGraphQLService.comments(annotationId, includeHidden, pagination);
  }

  /**
   * Adds a comment.
   *
   * @param input the comment input
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "addComment")
  @PreAuthorize("@permissions.has('graphql.hades.addComment')")
  public QueryResult addComment(CommentInput input) {
    return readerCommentGraphQLService.addComment(input);
  }

  /**
   * Updates a comment's body.
   *
   * @param id the comment id
   * @param body the new body
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "editComment")
  @PreAuthorize("@permissions.has('graphql.hades.editComment')")
  public QueryResult editComment(String id, String body) {
    return readerCommentGraphQLService.editComment(id, body);
  }

  /**
   * Deletes a comment.
   *
   * @param id the comment id
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "deleteComment")
  @PreAuthorize("@permissions.has('graphql.hades.deleteComment')")
  public QueryResult deleteComment(String id) {
    return readerCommentGraphQLService.deleteComment(id);
  }
}

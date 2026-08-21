package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.hades.codegen.types.PagedPrivateNotes;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.PrivateNoteInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.ShareNotesInput;
import com.sun.hades.graphql.services.PrivateNoteGraphQLService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for the reader queries and mutations.
 */
@DgsComponent
public class PrivateNoteDataFetcher {

  private final PrivateNoteGraphQLService privateNoteGraphQLService;

  public PrivateNoteDataFetcher(PrivateNoteGraphQLService privateNoteGraphQLService) {
    this.privateNoteGraphQLService = privateNoteGraphQLService;
  }

  /**
   * Paginated private notes for a text, visible to the current viewer.
   *
   * @param textId the text id
   * @param pagination the page request
   * @return the paged private notes
   */
  @DgsData(parentType = "HadesQueries", field = "privateNotes")
  @PreAuthorize("@permissions.has('graphql.hades.privateNotes')")
  public PagedPrivateNotes privateNotes(String textId, PaginationInput pagination) {
    return privateNoteGraphQLService.privateNotes(textId, pagination);
  }

  /**
   * Creates a private note on a range.
   *
   * @param input the private note input
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "createPrivateNote")
  @PreAuthorize("@permissions.has('graphql.hades.createPrivateNote')")
  public QueryResult createPrivateNote(PrivateNoteInput input) {
    return privateNoteGraphQLService.createPrivateNote(input);
  }

  /**
   * Deletes a private note (owner only).
   *
   * @param id the note id
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "deletePrivateNote")
  @PreAuthorize("@permissions.has('graphql.hades.deletePrivateNote')")
  public QueryResult deletePrivateNote(String id) {
    return privateNoteGraphQLService.deletePrivateNote(id);
  }

  /**
   * Shares all private notes on a text.
   *
   * @param input the share input
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "shareNotes")
  @PreAuthorize("@permissions.has('graphql.hades.shareNotes')")
  public QueryResult shareNotes(ShareNotesInput input) {
    return privateNoteGraphQLService.shareNotes(input);
  }
}

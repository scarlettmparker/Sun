package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.base.ratelimit.RateLimit;
import com.sun.hades.codegen.types.AnnotationInput;
import com.sun.hades.codegen.types.CommentInput;
import com.sun.hades.codegen.types.DiscordLoginResult;
import com.sun.hades.codegen.types.HadesMutations;
import com.sun.hades.codegen.types.HadesQueries;
import com.sun.hades.codegen.types.PagedReaderAnnotations;
import com.sun.hades.codegen.types.PagedReaderComments;
import com.sun.hades.codegen.types.PagedReaderTexts;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.TextLevelAssessment;
import com.sun.hades.codegen.types.Word;
import com.sun.hades.codegen.types.WordScope;
import com.sun.hades.codegen.types.PagedPrivateNotes;
import com.sun.hades.codegen.types.PrivateNoteInput;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.codegen.types.ReaderObjectReference;
import com.sun.hades.codegen.types.RemoteUserInput;
import com.sun.hades.codegen.types.ShareInput;
import com.sun.hades.codegen.types.VoteInput;
import com.sun.hades.graphql.services.HadesGraphQLService;
import com.sun.hades.graphql.services.WordReferenceService;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import com.sun.hades.codegen.types.ReaderAccount;

/**
 * Data fetchers for the reader queries and mutations.
 */
@DgsComponent
public class HadesDataFetcher {

  @Autowired
  private HadesGraphQLService hadesGraphQLService;

  @Autowired
  private WordReferenceService wordReferenceService;

  /**
   * Provides the reader queries object.
   *
   * @return a new HadesQueries instance
   */
  @DgsData(parentType = "Query", field = "hadesQueries")
  public HadesQueries getHadesQueries() {
    return HadesQueries.newBuilder().build();
  }

  /**
   * Lists texts, optionally filtered.
   *
   * @param level optional CEFR level
   * @param sourceId optional source id
   * @param ownerId optional owner account id
   * @param pagination the pagination input
   * @return a page of texts
   */
  @DgsData(parentType = "HadesQueries", field = "texts")
  @PreAuthorize("@permissions.has('graphql.hades.texts')")
  public PagedReaderTexts texts(PaginationInput pagination) {
    return hadesGraphQLService.texts(pagination);
  }

  /**
   * Locates a text by id.
   *
   * @param id the text id
   * @return the text
   */
  @DgsData(parentType = "HadesQueries", field = "text")
  @PreAuthorize("@permissions.has('graphql.hades.text')")
  public ReaderText text(String id) {
    return hadesGraphQLService.text(id);
  }

  /**
   * Resolves the heavy text body only when the client selects it, so list
   * queries that ask for id/title don't pull and serialize the full content.
   *
   * @param env the data-fetching environment, providing the parent ReaderText
   * @return the text content, or null when the parent has no id
   */
  @DgsData(parentType = "ReaderText", field = "content")
  @PreAuthorize("@permissions.has('graphql.hades.text')")
  public String textContent(DgsDataFetchingEnvironment env) {
    ReaderText parent = env.getSource();
    if (parent == null || parent.getId() == null) {
      return null;
    }
    return hadesGraphQLService.textContent(parent.getId());
  }

  /**
   * Predicts the CEFR level of a text.
   *
   * @param text the text to classify
   * @return the assessment
   */
  @DgsData(parentType = "HadesQueries", field = "classifyTextLevel")
  @PreAuthorize("@permissions.has('graphql.hades.classifyTextLevel')")
  public TextLevelAssessment classifyTextLevel(String text) {
    return hadesGraphQLService.classifyTextLevel(text);
  }

  /**
   * Defines a word from WordReference, honoring the requested scopes.
   *
   * @param word the headword to look up
   * @param scope the parts of the page to include
   * @return the word, or null when the entry does not exist
   */
  @DgsData(parentType = "HadesQueries", field = "defineWord")
  @PreAuthorize("@permissions.has('graphql.hades.defineWord')")
  public Word defineWord(String word, List<WordScope> scope) {
    return wordReferenceService.defineWord(word, scope);
  }

  /**
   * Locates a source by id.
   *
   * @param id the source id
   * @return the source
   */
  @DgsData(parentType = "HadesQueries", field = "source")
  @PreAuthorize("@permissions.has('graphql.hades.source')")
  public ReaderSource source(String id) {
    return hadesGraphQLService.source(id);
  }

  /**
   * Lists all sources.
   *
   * @return the sources
   */
  @DgsData(parentType = "HadesQueries", field = "sources")
  @PreAuthorize("@permissions.has('graphql.hades.sources')")
  public List<ReaderSource> sources() {
    return hadesGraphQLService.sources();
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
    return hadesGraphQLService.annotations(textId, includeHidden, pagination);
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
    return hadesGraphQLService.annotation(id);
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
    return hadesGraphQLService.comments(annotationId, includeHidden, pagination);
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
    return hadesGraphQLService.privateNotes(textId, pagination);
  }

  /**
   * Returns the current member's reader account, or null when unauthenticated.
   *
   * @return the reader account
   */
  @DgsData(parentType = "HadesQueries", field = "readerAccount")
  @PreAuthorize("permitAll()")
  public ReaderAccount readerAccount() {
    return hadesGraphQLService.readerAccount();
  }

  /**
   * Locates reader accounts for a set of remote users.
   *
   * @param remoteUsers the remote-user references
   * @return the matching reader accounts
   */
  @DgsData(parentType = "HadesQueries", field = "readerAccounts")
  @PreAuthorize("@permissions.has('graphql.hades.readerAccounts')")
  public List<ReaderAccount> readerAccounts(
      List<RemoteUserInput> remoteUsers) {
    return hadesGraphQLService.readerAccounts(remoteUsers);
  }

  /**
   * Searches reader accounts by username.
   *
   * @param query the username fragment
   * @param pagination the page request
   * @return the matching reader accounts
   */
  @DgsData(parentType = "HadesQueries", field = "searchReaderAccounts")
  @PreAuthorize("@permissions.has('graphql.hades.searchReaderAccounts')")
  public List<ReaderAccount> searchReaderAccounts(String query, PaginationInput pagination) {
    return hadesGraphQLService.searchReaderAccounts(query, pagination);
  }

  /**
   * Returns the caller's vote on a target.
   *
   * @param targetType the target type
   * @param targetId the target id
   * @return the vote value
   */
  @DgsData(parentType = "HadesQueries", field = "myVote")
  @PreAuthorize("@permissions.has('graphql.hades.myVote')")
  public VoteValue myVote(ReaderVoteTarget targetType, String targetId) {
    return hadesGraphQLService.myVote(targetType, targetId);
  }

  /**
   * Finds annotations referencing any of the given remote object ids.
   *
   * @param ids the remote object ids
   * @return the references
   */
  @DgsData(parentType = "HadesQueries", field = "locateRemoteObjects")
  @PreAuthorize("@permissions.has('graphql.hades.locateRemoteObjects')")
  public List<ReaderObjectReference> locateRemoteObjects(List<String> ids) {
    return hadesGraphQLService.locateRemoteObjects(ids);
  }

  /**
   * Provides the reader mutations object.
   *
   * @return a new HadesMutations instance
   */
  @DgsData(parentType = "Mutation", field = "hadesMutations")
  public HadesMutations getHadesMutations() {
    return HadesMutations.newBuilder().build();
  }

  /**
   * Creates a source.
   *
   * @param name the source name
   * @param url the source url
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "createSource")
  @PreAuthorize("@permissions.has('graphql.hades.createSource')")
  public QueryResult createSource(String name, String url) {
    return hadesGraphQLService.createSource(name, url);
  }

  /**
   * Creates a text.
   *
   * @param input the text input
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "createText")
  @PreAuthorize("@permissions.has('graphql.hades.createText')")
  public QueryResult createText(ReaderTextInput input) {
    return hadesGraphQLService.createText(input);
  }

  /**
   * Archives a text.
   *
   * @param id the text id
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "archiveText")
  @PreAuthorize("@permissions.has('graphql.hades.archiveText')")
  public QueryResult archiveText(String id) {
    return hadesGraphQLService.archiveText(id);
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
    return hadesGraphQLService.createAnnotation(
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
    return hadesGraphQLService.editAnnotation(id, body);
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
    return hadesGraphQLService.deleteAnnotation(id);
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
    return hadesGraphQLService.addComment(input);
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
    return hadesGraphQLService.editComment(id, body);
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
    return hadesGraphQLService.deleteComment(id);
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
    return hadesGraphQLService.createPrivateNote(input);
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
    return hadesGraphQLService.deletePrivateNote(id);
  }

  /**
   * Shares a private note with a subject.
   *
   * @param input the share input
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "sharePrivateNote")
  @PreAuthorize("@permissions.has('graphql.hades.sharePrivateNote')")
  public QueryResult sharePrivateNote(ShareInput input) {
    return hadesGraphQLService.sharePrivateNote(input);
  }

  /**
   * Casts a vote.
   *
   * @param input the vote input
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "vote")
  @PreAuthorize("@permissions.has('graphql.hades.vote')")
  public QueryResult vote(VoteInput input) {
    return hadesGraphQLService.vote(input);
  }

  /**
   * Removes the caller's vote.
   *
   * @param targetType the target type
   * @param targetId the target id
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "removeVote")
  @PreAuthorize("@permissions.has('graphql.hades.removeVote')")
  public QueryResult removeVote(ReaderVoteTarget targetType, String targetId) {
    return hadesGraphQLService.removeVote(targetType, targetId);
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
    return hadesGraphQLService.attachObject(source, target);
  }

  /**
   * Exchanges a Discord authorization code for a JWT.
   *
   * @param code the authorization code
   * @param state the OAuth state token
   * @return the login result
   */
  @DgsData(parentType = "HadesMutations", field = "discordLogin")
  @PreAuthorize("permitAll()")
  public DiscordLoginResult discordLogin(String code, String state) {
    return hadesGraphQLService.discordLogin(code, state);
  }
}

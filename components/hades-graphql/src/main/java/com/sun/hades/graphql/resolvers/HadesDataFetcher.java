package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.hades.codegen.types.AnnotationInput;
import com.sun.hades.codegen.types.CommentInput;
import com.sun.hades.codegen.types.DiscordLoginResult;
import com.sun.hades.codegen.types.HadesMutations;
import com.sun.hades.codegen.types.HadesQueries;
import com.sun.hades.codegen.types.PagedReaderComments;
import com.sun.hades.codegen.types.PagedReaderTexts;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.QueryResult;
import com.sun.hades.codegen.types.ReaderAnnotation;
import com.sun.hades.codegen.types.ReaderSource;
import com.sun.hades.codegen.types.ReaderText;
import com.sun.hades.codegen.types.ReaderTextInput;
import com.sun.hades.codegen.types.ReaderObjectReference;
import com.sun.hades.codegen.types.RemoteUserInput;
import com.sun.hades.codegen.types.VoteInput;
import com.sun.hades.graphql.services.HadesGraphQLService;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Data fetchers for the reader queries and mutations.
 */
@DgsComponent
public class HadesDataFetcher {

  @Autowired
  private HadesGraphQLService hadesGraphQLService;

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
  public ReaderText text(String id) {
    return hadesGraphQLService.text(id);
  }

  /**
   * Locates a source by id.
   *
   * @param id the source id
   * @return the source
   */
  @DgsData(parentType = "HadesQueries", field = "source")
  public ReaderSource source(String id) {
    return hadesGraphQLService.source(id);
  }

  /**
   * Lists all sources.
   *
   * @return the sources
   */
  @DgsData(parentType = "HadesQueries", field = "sources")
  public List<ReaderSource> sources() {
    return hadesGraphQLService.sources();
  }

  /**
   * Lists annotations for a text.
   *
   * @param textId the text id
   * @param includeHidden whether to include hidden annotations
   * @return the annotations
   */
  @DgsData(parentType = "HadesQueries", field = "annotations")
  public List<ReaderAnnotation> annotations(String textId, Boolean includeHidden) {
    return hadesGraphQLService.annotations(textId, includeHidden);
  }

  /**
   * Locates an annotation by id.
   *
   * @param id the annotation id
   * @return the annotation
   */
  @DgsData(parentType = "HadesQueries", field = "annotation")
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
  public PagedReaderComments comments(String annotationId, Boolean includeHidden, PaginationInput pagination) {
    return hadesGraphQLService.comments(annotationId, includeHidden, pagination);
  }

  /**
   * Returns the current member's reader account.
   *
   * @return the reader account
   */
  @DgsData(parentType = "HadesQueries", field = "readerAccount")
  public com.sun.hades.codegen.types.ReaderAccount readerAccount() {
    return hadesGraphQLService.readerAccount();
  }

  /**
   * Locates reader accounts for a set of remote users.
   *
   * @param remoteUsers the remote-user references
   * @return the matching reader accounts
   */
  @DgsData(parentType = "HadesQueries", field = "readerAccounts")
  public List<com.sun.hades.codegen.types.ReaderAccount> readerAccounts(
      List<RemoteUserInput> remoteUsers) {
    return hadesGraphQLService.readerAccounts(remoteUsers);
  }

  /**
   * Returns the caller's vote on a target.
   *
   * @param targetType the target type
   * @param targetId the target id
   * @return the vote value
   */
  @DgsData(parentType = "HadesQueries", field = "myVote")
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
  public QueryResult deleteComment(String id) {
    return hadesGraphQLService.deleteComment(id);
  }

  /**
   * Casts a vote.
   *
   * @param input the vote input
   * @return a QueryResult
   */
  @DgsData(parentType = "HadesMutations", field = "vote")
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
  public DiscordLoginResult discordLogin(String code, String state) {
    return hadesGraphQLService.discordLogin(code, state);
  }
}

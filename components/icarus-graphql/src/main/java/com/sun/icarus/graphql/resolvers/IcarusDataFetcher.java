package com.sun.icarus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.icarus.codegen.types.CreatePostInput;
import com.sun.icarus.codegen.types.CreateThreadInput;
import com.sun.icarus.codegen.types.ForumPost;
import com.sun.icarus.codegen.types.ForumThread;
import com.sun.icarus.codegen.types.IcarusMutations;
import com.sun.icarus.codegen.types.IcarusQueries;
import com.sun.icarus.codegen.types.PagedForumPosts;
import com.sun.icarus.codegen.types.PagedForumThreads;
import com.sun.icarus.codegen.types.PaginationInput;
import com.sun.icarus.codegen.types.QueryResult;
import com.sun.icarus.codegen.types.RemoteObjectReference;
import com.sun.icarus.codegen.types.VoteInput;
import com.sun.icarus.graphql.services.IcarusGraphQLService;
import com.sun.icarus.model.enums.VoteValue;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Data fetchers for the forum queries and mutations.
 */
@DgsComponent
public class IcarusDataFetcher {

  @Autowired
  private IcarusGraphQLService icarusGraphQLService;

  /**
   * Provides the forum queries object.
   *
   * @return a new IcarusQueries instance
   */
  @DgsData(parentType = "Query", field = "icarusQueries")
  public IcarusQueries getIcarusQueries() {
    return IcarusQueries.newBuilder().build();
  }

  /**
   * Locates a thread by id.
   *
   * @param id the thread id
   * @return the thread
   */
  @DgsData(parentType = "IcarusQueries", field = "thread")
  public ForumThread thread(String id) {
    return icarusGraphQLService.thread(id);
  }

  /**
   * Lists threads attached to a remote object.
   *
   * @param remoteObject the remote object id
   * @return a page of threads
   */
  @DgsData(parentType = "IcarusQueries", field = "threadsFor")
  public PagedForumThreads threadsFor(String remoteObject) {
    return icarusGraphQLService.threadsFor(remoteObject);
  }

  /**
   * Lists posts in a thread.
   *
   * @param threadId the thread id
   * @param includeHidden whether to include hidden posts
   * @param pagination the pagination input
   * @return a page of posts
   */
  @DgsData(parentType = "IcarusQueries", field = "posts")
  public PagedForumPosts posts(String threadId, Boolean includeHidden, PaginationInput pagination) {
    return icarusGraphQLService.posts(threadId, includeHidden, pagination);
  }

  /**
   * Returns the caller's vote on a post.
   *
   * @param postId the post id
   * @return the vote value
   */
  @DgsData(parentType = "IcarusQueries", field = "myVote")
  public VoteValue myVote(String postId) {
    return icarusGraphQLService.myVote(postId);
  }

  /**
   * Finds threads referencing any of the given remote object ids.
   *
   * @param ids the remote object ids
   * @return the references
   */
  @DgsData(parentType = "IcarusQueries", field = "locateRemoteObjects")
  public List<RemoteObjectReference> locateRemoteObjects(List<String> ids) {
    return icarusGraphQLService.locateRemoteObjects(ids);
  }

  /**
   * Provides the forum mutations object.
   *
   * @return a new IcarusMutations instance
   */
  @DgsData(parentType = "Mutation", field = "icarusMutations")
  public IcarusMutations getIcarusMutations() {
    return IcarusMutations.newBuilder().build();
  }

  /**
   * Creates a thread attached to a remote object.
   *
   * @param input the create thread input
   * @return a QueryResult
   */
  @DgsData(parentType = "IcarusMutations", field = "createThread")
  public QueryResult createThread(CreateThreadInput input) {
    return icarusGraphQLService.createThread(input);
  }

  /**
   * Locks a thread.
   *
   * @param id the thread id
   * @return a QueryResult
   */
  @DgsData(parentType = "IcarusMutations", field = "lockThread")
  public QueryResult lockThread(String id) {
    return icarusGraphQLService.lockThread(id);
  }

  /**
   * Archives a thread.
   *
   * @param id the thread id
   * @return a QueryResult
   */
  @DgsData(parentType = "IcarusMutations", field = "archiveThread")
  public QueryResult archiveThread(String id) {
    return icarusGraphQLService.archiveThread(id);
  }

  /**
   * Adds a post to a thread.
   *
   * @param input the create post input
   * @return a QueryResult
   */
  @DgsData(parentType = "IcarusMutations", field = "createPost")
  public QueryResult createPost(CreatePostInput input) {
    return icarusGraphQLService.createPost(input);
  }

  /**
   * Updates a post's body.
   *
   * @param id the post id
   * @param body the new body
   * @return a QueryResult
   */
  @DgsData(parentType = "IcarusMutations", field = "editPost")
  public QueryResult editPost(String id, String body) {
    return icarusGraphQLService.editPost(id, body);
  }

  /**
   * Soft-deletes a post.
   *
   * @param id the post id
   * @return a QueryResult
   */
  @DgsData(parentType = "IcarusMutations", field = "deletePost")
  public QueryResult deletePost(String id) {
    return icarusGraphQLService.deletePost(id);
  }

  /**
   * Casts a vote on a post.
   *
   * @param input the vote input
   * @return a QueryResult
   */
  @DgsData(parentType = "IcarusMutations", field = "vote")
  public QueryResult vote(VoteInput input) {
    return icarusGraphQLService.vote(input);
  }

  /**
   * Removes the caller's vote on a post.
   *
   * @param postId the post id
   * @return a QueryResult
   */
  @DgsData(parentType = "IcarusMutations", field = "removeVote")
  public QueryResult removeVote(String postId) {
    return icarusGraphQLService.removeVote(postId);
  }

  /**
   * Attaches a remote object id to a thread.
   *
   * @param source the thread id
   * @param target the remote object id
   * @return a QueryResult
   */
  @DgsData(parentType = "IcarusMutations", field = "attachObject")
  public QueryResult attachObject(String source, String target) {
    return icarusGraphQLService.attachObject(source, target);
  }
}

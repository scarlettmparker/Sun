package com.sun.icarus.graphql.services;

import com.sun.base.util.PageRequests;
import com.sun.icarus.codegen.types.CreatePostInput;
import com.sun.icarus.codegen.types.CreateThreadInput;
import com.sun.icarus.codegen.types.ForumPost;
import com.sun.icarus.codegen.types.ForumThread;
import com.sun.icarus.codegen.types.PageInfo;
import com.sun.icarus.codegen.types.PagedForumPosts;
import com.sun.icarus.codegen.types.PagedForumThreads;
import com.sun.icarus.codegen.types.PaginationInput;
import com.sun.icarus.codegen.types.QueryResult;
import com.sun.icarus.codegen.types.QuerySuccess;
import com.sun.icarus.codegen.types.RemoteObjectReference;
import com.sun.icarus.codegen.types.StandardError;
import com.sun.icarus.codegen.types.VoteInput;
import com.sun.icarus.graphql.mappers.ForumPostMapper;
import com.sun.icarus.graphql.mappers.ForumThreadMapper;
import com.sun.icarus.model.ForumPostEntity;
import com.sun.icarus.model.enums.PostStatus;
import com.sun.icarus.model.enums.ThreadStatus;
import com.sun.icarus.model.enums.VoteValue;
import com.sun.icarus.service.ForumPostService;
import com.sun.icarus.service.ForumThreadService;
import com.sun.icarus.service.ForumVoteService;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for discussion forums.
 */
@Service
public class IcarusGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(IcarusGraphQLService.class);

  private final ForumThreadService threadService;
  private final ForumPostService postService;
  private final ForumVoteService voteService;
  private final ForumThreadMapper threadMapper;
  private final ForumPostMapper postMapper;

  public IcarusGraphQLService(ForumThreadService threadService, ForumPostService postService,
      ForumVoteService voteService, ForumThreadMapper threadMapper, ForumPostMapper postMapper) {
    this.threadService = threadService;
    this.postService = postService;
    this.voteService = voteService;
    this.threadMapper = threadMapper;
    this.postMapper = postMapper;
  }

  /**
   * Locates a thread by id.
   *
   * @param id the thread id
   * @return the thread, or null
   */
  @Transactional(value = "icarusTransactionManager", readOnly = true)
  public ForumThread thread(String id) {
    return threadService.findById(UUID.fromString(id)).map(threadMapper::map).orElse(null);
  }

  /**
   * Lists threads attached to a remote object.
   *
   * @param remoteObject the remote object id
   * @return a page of threads
   */
  @Transactional(value = "icarusTransactionManager", readOnly = true)
  public PagedForumThreads threadsFor(String remoteObject) {
    List<ForumThread> items =
        threadService.listForRemoteObject(remoteObject).stream().map(threadMapper::map).toList();
    return PagedForumThreads.newBuilder()
        .items(items)
        .pageInfo(PageInfo.newBuilder()
            .page(0)
            .size(items.size())
            .totalPages(1)
            .totalCount(items.size())
            .hasNextPage(false)
            .hasPreviousPage(false)
            .build())
        .build();
  }

  /**
   * Lists posts in a thread.
   *
   * @param threadId the thread id
   * @param includeHidden whether to include hidden or deleted posts
   * @param pagination the pagination input
   * @return a page of posts
   */
  @Transactional(value = "icarusTransactionManager", readOnly = true)
  public PagedForumPosts posts(String threadId, Boolean includeHidden, PaginationInput pagination) {
    Pageable pageable = toPageable(pagination, "createdAt", Sort.Direction.ASC);
    Page<ForumPostEntity> result = postService.listForThread(UUID.fromString(threadId), pageable);
    List<ForumPost> items = result.getContent().stream()
        .filter(p -> Boolean.TRUE.equals(includeHidden) || p.getStatus() == PostStatus.ACTIVE)
        .map(postMapper::map)
        .toList();
    return PagedForumPosts.newBuilder().items(items).pageInfo(pageInfo(result)).build();
  }

  /**
   * Returns the caller's vote on a post.
   *
   * @param postId the post id
   * @return the vote value, or null
   */
  @Transactional(value = "icarusTransactionManager", readOnly = true)
  public VoteValue myVote(String postId) {
    return voteService.myVote(UUID.fromString(postId)).orElse(null);
  }

  /**
   * Finds threads referencing any of the given remote object ids.
   *
   * @param ids the remote object ids
   * @return the references
   */
  @Transactional(value = "icarusTransactionManager", readOnly = true)
  public List<RemoteObjectReference> locateRemoteObjects(List<String> ids) {
    return threadService.locateRemoteObjects(ids).stream()
        .map(r -> RemoteObjectReference.newBuilder()
            .id(r.id().toString())
            .ownerType(r.ownerType())
            .ownerId(r.ownerId().toString())
            .build())
        .toList();
  }

  /**
   * Creates a thread attached to a remote object.
   *
   * @param input the create thread input
   * @return a QueryResult
   */
  @Transactional("icarusTransactionManager")
  public QueryResult createThread(CreateThreadInput input) {
    return mutate("createThread",
        () -> threadService.create(input.getTitle(), input.getRemoteObject()));
  }

  /**
   * Locks a thread.
   *
   * @param id the thread id
   * @return a QueryResult
   */
  @Transactional("icarusTransactionManager")
  public QueryResult lockThread(String id) {
    return mutate("lockThread",
        () -> threadService.setStatus(UUID.fromString(id), ThreadStatus.LOCKED));
  }

  /**
   * Archives a thread.
   *
   * @param id the thread id
   * @return a QueryResult
   */
  @Transactional("icarusTransactionManager")
  public QueryResult archiveThread(String id) {
    return mutate("archiveThread",
        () -> threadService.setStatus(UUID.fromString(id), ThreadStatus.ARCHIVED));
  }

  /**
   * Adds a post to a thread.
   *
   * @param input the create post input
   * @return a QueryResult
   */
  @Transactional("icarusTransactionManager")
  public QueryResult createPost(CreatePostInput input) {
    return mutate("createPost", () -> postService.addPost(
        UUID.fromString(input.getThreadId()),
        input.getParentId() != null ? UUID.fromString(input.getParentId()) : null,
        input.getBody()));
  }

  /**
   * Updates a post's body.
   *
   * @param id the post id
   * @param body the new body
   * @return a QueryResult
   */
  @Transactional("icarusTransactionManager")
  public QueryResult editPost(String id, String body) {
    return mutate("editPost", () -> postService.editPost(UUID.fromString(id), body));
  }

  /**
   * Soft-deletes a post.
   *
   * @param id the post id
   * @return a QueryResult
   */
  @Transactional("icarusTransactionManager")
  public QueryResult deletePost(String id) {
    return mutate("deletePost", () -> postService.deletePost(UUID.fromString(id)));
  }

  /**
   * Casts a vote on a post.
   *
   * @param input the vote input
   * @return a QueryResult
   */
  @Transactional("icarusTransactionManager")
  public QueryResult vote(VoteInput input) {
    return mutate("vote", () -> voteService.vote(
        UUID.fromString(input.getPostId()), input.getValue()));
  }

  /**
   * Removes the caller's vote on a post.
   *
   * @param postId the post id
   * @return a QueryResult
   */
  @Transactional("icarusTransactionManager")
  public QueryResult removeVote(String postId) {
    return mutate("removeVote", () -> voteService.removeVote(UUID.fromString(postId)));
  }

  /**
   * Attaches a remote object id to a thread.
   *
   * @param source the thread id
   * @param target the remote object id
   * @return a QueryResult
   */
  @Transactional("icarusTransactionManager")
  public QueryResult attachObject(String source, String target) {
    return mutate("attachObject",
        () -> threadService.attach(UUID.fromString(source), target));
  }

  /**
   * Converts the pagination input into a pageable, applying the given defaults.
   *
   * @param pagination the pagination and sort input
   * @param defaultSortBy the property to sort by when none is given
   * @param defaultDir the direction when none is given
   * @return the pageable
   */
  private Pageable toPageable(PaginationInput pagination, String defaultSortBy, Sort.Direction defaultDir) {
    if (pagination == null) {
      return PageRequests.of(null, null, null, null, defaultSortBy, defaultDir);
    }
    return PageRequests.of(pagination.getPage(), pagination.getSize(), pagination.getSortBy(),
        pagination.getSortDir() != null ? pagination.getSortDir().name() : null,
        defaultSortBy, defaultDir);
  }

  /**
   * Builds page metadata from a Spring data page.
   *
   * @param result the data page
   * @return the GraphQL PageInfo
   */
  private PageInfo pageInfo(Page<?> result) {
    return PageInfo.newBuilder()
        .page(result.getNumber())
        .size(result.getSize())
        .totalPages(result.getTotalPages())
        .totalCount((int) result.getTotalElements())
        .hasNextPage(result.hasNext())
        .hasPreviousPage(result.hasPrevious())
        .build();
  }

  /**
   * Runs a mutation, returning QuerySuccess with the affected id or StandardError
   * on failure.
   *
   * @param op the operation name (for logging and messages)
   * @param action the mutation, returning the affected entity id
   * @return a QueryResult
   */
  private QueryResult mutate(String op, Supplier<UUID> action) {
    try {
      UUID id = action.get();
      logger.info("{} succeeded for id {}", op, id);
      return QuerySuccess.newBuilder()
          .message(op + " succeeded")
          .id(id != null ? id.toString() : null)
          .build();
    } catch (Exception e) {
      logger.error("{} failed", op, e);
      return StandardError.newBuilder()
          .message(op + " failed: " + e.getMessage())
          .build();
    }
  }
}

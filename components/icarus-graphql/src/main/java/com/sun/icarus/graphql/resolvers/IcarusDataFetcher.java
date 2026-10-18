package com.sun.icarus.graphql.resolvers;
import com.sun.base.audit.annotation.Audited;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.util.PageRequests;
import com.sun.gaia.service.AccountService;
import com.sun.icarus.codegen.types.ArchiveThreadResponse;
import com.sun.icarus.codegen.types.AttachForumObjectResponse;
import com.sun.icarus.codegen.types.CreatePostInput;
import com.sun.icarus.codegen.types.CreatePostResponse;
import com.sun.icarus.codegen.types.CreateThreadInput;
import com.sun.icarus.codegen.types.CreateThreadResponse;
import com.sun.icarus.codegen.types.DeletePostResponse;
import com.sun.icarus.codegen.types.EditPostResponse;
import com.sun.icarus.codegen.types.ForumObjectReference;
import com.sun.icarus.codegen.types.ForumPost;
import com.sun.icarus.codegen.types.ForumThread;
import com.sun.icarus.codegen.types.ForumVoteInput;
import com.sun.icarus.codegen.types.IcarusMutations;
import com.sun.icarus.codegen.types.IcarusQueries;
import com.sun.icarus.codegen.types.LockThreadResponse;
import com.sun.icarus.codegen.types.PageInfo;
import com.sun.icarus.codegen.types.PagedForumPosts;
import com.sun.icarus.codegen.types.PagedForumThreads;
import com.sun.icarus.codegen.types.PaginationInput;
import com.sun.icarus.codegen.types.RemoteUser;
import com.sun.icarus.codegen.types.RemoteUserType;
import com.sun.icarus.codegen.types.RemoveVoteResponse;
import com.sun.icarus.codegen.types.VoteForumResponse;
import com.sun.icarus.graphql.mappers.ForumPostMapper;
import com.sun.icarus.graphql.mappers.ForumThreadMapper;
import com.sun.icarus.model.ForumPostEntity;
import com.sun.icarus.model.ForumThreadEntity;
import com.sun.icarus.model.enums.PostStatus;
import com.sun.icarus.model.enums.ThreadStatus;
import com.sun.icarus.model.enums.VoteValue;
import com.sun.icarus.service.ForumPostService;
import com.sun.icarus.service.ForumThreadService;
import com.sun.icarus.service.ForumVoteService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import com.sun.base.audit.annotation.NotAudited;
/**
 * Data fetchers for the forum queries and mutations.
 */
@DgsComponent
public class IcarusDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(IcarusDataFetcher.class);

  private final ForumThreadService threadService;
  private final ForumPostService postService;
  private final ForumVoteService voteService;
  private final ForumThreadMapper threadMapper;
  private final ForumPostMapper postMapper;
  private final AccountService accountService;

  public IcarusDataFetcher(ForumThreadService threadService, ForumPostService postService,
      ForumVoteService voteService, ForumThreadMapper threadMapper, ForumPostMapper postMapper,
      AccountService accountService) {
    this.threadService = threadService;
    this.postService = postService;
    this.voteService = voteService;
    this.threadMapper = threadMapper;
    this.postMapper = postMapper;
    this.accountService = accountService;
  }

  /**
   * Provides the forum queries object.
   *
   * @return a new IcarusQueries instance
   */
  @NotAudited(reason = "namespace root")
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
  @Audited(event = "THREAD")
  @DgsData(parentType = "IcarusQueries", field = "thread")
  @PreAuthorize("@permissions.has('graphql.icarus.thread')")
  @Transactional(readOnly = true)
  public ForumThread thread(String id) {
    return threadService.findById(UUID.fromString(id)).map(threadMapper::map).orElse(null);
  }

  /**
   * Lists threads attached to a remote object.
   *
   * @param remoteObject the remote object id
   * @return a page of threads
   */
  @Audited(event = "THREADSFOR")
  @DgsData(parentType = "IcarusQueries", field = "threadsFor")
  @PreAuthorize("@permissions.has('graphql.icarus.threadsFor')")
  @Transactional(readOnly = true)
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
   * @param includeHidden whether to include hidden posts
   * @param pagination the pagination input
   * @return a page of posts
   */
  @Audited(event = "POSTS")
  @DgsData(parentType = "IcarusQueries", field = "posts")
  @PreAuthorize("@permissions.has('graphql.icarus.posts')")
  @Transactional(readOnly = true)
  public PagedForumPosts posts(String threadId, Boolean includeHidden, PaginationInput pagination) {
    Pageable pageable = toPageable(pagination, "createdAt", Sort.Direction.ASC);
    Page<ForumPostEntity> result = postService.listForThread(UUID.fromString(threadId), pageable);
    List<ForumPostEntity> visible = result.getContent().stream()
        .filter(p -> Boolean.TRUE.equals(includeHidden) || p.getStatus() == PostStatus.ACTIVE)
        .toList();
    Map<UUID, RemoteUser> authors = new HashMap<>();
    visible.stream()
        .map(ForumPostEntity::getCreatedBy)
        .filter(Objects::nonNull)
        .distinct()
        .forEach(gaiaAccountId -> accountService.findById(gaiaAccountId).ifPresent(acc -> {
          if ("discord".equals(acc.getProvider()) && acc.getProviderId() != null) {
            authors.put(gaiaAccountId, remoteUser(acc.getProviderId()));
          }
        }));
    Map<UUID, VoteValue> myVotes = voteService.myVotes(
        visible.stream().map(ForumPostEntity::getId).toList());
    List<ForumPost> items = visible.stream()
        .map(p -> postMapper.map(p, authors.get(p.getCreatedBy()), myVotes.get(p.getId())))
        .toList();
    return PagedForumPosts.newBuilder().items(items).pageInfo(pageInfo(result)).build();
  }

  /**
   * Returns the caller's vote on a post.
   *
   * @param postId the post id
   * @return the vote value
   */
  @Audited(event = "MYVOTE")
  @DgsData(parentType = "IcarusQueries", field = "myVote")
  @PreAuthorize("@permissions.has('graphql.icarus.myVote')")
  @Transactional(readOnly = true)
  public VoteValue myVote(String postId) {
    return voteService.myVote(UUID.fromString(postId)).orElse(null);
  }

  /**
   * Finds threads referencing any of the given remote object ids.
   *
   * @param ids the remote object ids
   * @return the references
   */
  @Audited(event = "LOCATEREMOTEOBJECTS", entity = "RemoteObject")
  @DgsData(parentType = "IcarusQueries", field = "locateRemoteObjects")
  @PreAuthorize("@permissions.has('graphql.icarus.locateRemoteObjects')")
  @Transactional(readOnly = true)
  public List<ForumObjectReference> locateRemoteObjects(List<String> ids) {
    return threadService.locateRemoteObjects(ids).stream()
        .map(r -> ForumObjectReference.newBuilder()
            .id(r.id().toString())
            .ownerType(r.ownerType())
            .ownerId(r.ownerId().toString())
            .build())
        .toList();
  }

  /**
   * Provides the forum mutations object.
   *
   * @return a new IcarusMutations instance
   */
  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Mutation", field = "icarusMutations")
  public IcarusMutations getIcarusMutations() {
    return IcarusMutations.newBuilder().build();
  }

  /**
   * Creates a thread attached to a remote object.
   *
   * @param input the create thread input
   * @return the created thread
   */
  @Audited(event = "CREATETHREAD", entity = "Thread")
  @DgsData(parentType = "IcarusMutations", field = "createThread")
  @PreAuthorize("@permissions.has('graphql.icarus.createThread')")
  @Transactional
  public CreateThreadResponse createThread(CreateThreadInput input) {
    UUID id = threadService.create(input.getTitle(), input.getRemoteObject());
    ForumThreadEntity thread = threadService.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + id));
    logger.info("createThread succeeded for id {}", id);
    return CreateThreadResponse.newBuilder()
        .message("createThread succeeded")
        .thread(threadMapper.map(thread))
        .build();
  }

  /**
   * Locks a thread.
   *
   * @param id the thread id
   * @return the updated thread
   */
  @Audited(event = "LOCKTHREAD", entity = "Thread")
  @DgsData(parentType = "IcarusMutations", field = "lockThread")
  @PreAuthorize("@permissions.has('graphql.icarus.lockThread')")
  @Transactional
  public LockThreadResponse lockThread(String id) {
    UUID threadId = threadService.setStatus(UUID.fromString(id), ThreadStatus.LOCKED);
    ForumThreadEntity thread = threadService.findById(threadId)
        .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));
    logger.info("lockThread succeeded for id {}", threadId);
    return LockThreadResponse.newBuilder()
        .message("lockThread succeeded")
        .thread(threadMapper.map(thread))
        .build();
  }

  /**
   * Archives a thread.
   *
   * @param id the thread id
   * @return the updated thread
   */
  @Audited(event = "ARCHIVETHREAD", entity = "Thread")
  @DgsData(parentType = "IcarusMutations", field = "archiveThread")
  @PreAuthorize("@permissions.has('graphql.icarus.archiveThread')")
  @Transactional
  public ArchiveThreadResponse archiveThread(String id) {
    UUID threadId = threadService.setStatus(UUID.fromString(id), ThreadStatus.ARCHIVED);
    ForumThreadEntity thread = threadService.findById(threadId)
        .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));
    logger.info("archiveThread succeeded for id {}", threadId);
    return ArchiveThreadResponse.newBuilder()
        .message("archiveThread succeeded")
        .thread(threadMapper.map(thread))
        .build();
  }

  /**
   * Adds a post to a thread.
   *
   * @param input the create post input
   * @return the created post
   */
  @Audited(event = "CREATEPOST", entity = "Post")
  @DgsData(parentType = "IcarusMutations", field = "createPost")
  @PreAuthorize("@permissions.has('graphql.icarus.createPost')")
  @Transactional
  public CreatePostResponse createPost(CreatePostInput input) {
    UUID id = postService.addPost(
        UUID.fromString(input.getThreadId()),
        input.getParentId() == null ? null : UUID.fromString(input.getParentId()),
        input.getBody());
    ForumPost post = mapPost(id);
    logger.info("createPost succeeded for id {}", id);
    return CreatePostResponse.newBuilder()
        .message("createPost succeeded")
        .post(post)
        .build();
  }

  /**
   * Updates a post's body.
   *
   * @param id the post id
   * @param body the new body
   * @return the updated post
   */
  @Audited(event = "EDITPOST", entity = "Post")
  @DgsData(parentType = "IcarusMutations", field = "editPost")
  @PreAuthorize("@permissions.has('graphql.icarus.editPost')")
  @Transactional
  public EditPostResponse editPost(String id, String body) {
    UUID postId = postService.editPost(UUID.fromString(id), body);
    ForumPost post = mapPost(postId);
    logger.info("editPost succeeded for id {}", postId);
    return EditPostResponse.newBuilder()
        .message("editPost succeeded")
        .post(post)
        .build();
  }

  /**
   * Soft-deletes a post.
   *
   * @param id the post id
   * @return the deleted post id
   */
  @Audited(event = "DELETEPOST", entity = "Post")
  @DgsData(parentType = "IcarusMutations", field = "deletePost")
  @PreAuthorize("@permissions.has('graphql.icarus.deletePost')")
  @Transactional
  public DeletePostResponse deletePost(String id) {
    UUID postId = postService.deletePost(UUID.fromString(id));
    logger.info("deletePost succeeded for id {}", postId);
    return DeletePostResponse.newBuilder()
        .message("deletePost succeeded")
        .id(postId.toString())
        .build();
  }

  /**
   * Casts a vote on a post.
   *
   * @param input the vote input
   * @return the voted post
   */
  @Audited(event = "VOTE")
  @DgsData(parentType = "IcarusMutations", field = "vote")
  @PreAuthorize("@permissions.has('graphql.icarus.vote')")
  @Transactional
  public VoteForumResponse vote(ForumVoteInput input) {
    UUID postId = voteService.vote(UUID.fromString(input.getPostId()), input.getValue());
    ForumPost post = mapPost(postId);
    logger.info("vote succeeded for id {}", postId);
    return VoteForumResponse.newBuilder()
        .message("vote succeeded")
        .post(post)
        .build();
  }

  /**
   * Removes the caller's vote on a post.
   *
   * @param postId the post id
   * @return the updated post
   */
  @Audited(event = "REMOVEVOTE", entity = "Vote")
  @DgsData(parentType = "IcarusMutations", field = "removeVote")
  @PreAuthorize("@permissions.has('graphql.icarus.removeVote')")
  @Transactional
  public RemoveVoteResponse removeVote(String postId) {
    UUID id = voteService.removeVote(UUID.fromString(postId));
    ForumPost post = mapPost(id);
    logger.info("removeVote succeeded for id {}", id);
    return RemoveVoteResponse.newBuilder()
        .message("removeVote succeeded")
        .post(post)
        .build();
  }

  /**
   * Attaches a remote object id to a thread.
   *
   * @param source the thread id
   * @param target the remote object id
   * @return the thread id
   */
  @Audited(event = "ATTACHOBJECT", entity = "Object")
  @DgsData(parentType = "IcarusMutations", field = "attachObject")
  @PreAuthorize("@permissions.has('graphql.icarus.attachObject')")
  @Transactional
  public AttachForumObjectResponse attachObject(String source, String target) {
    UUID id = threadService.attach(UUID.fromString(source), target);
    logger.info("attachObject succeeded for id {}", id);
    return AttachForumObjectResponse.newBuilder()
        .message("attachObject succeeded")
        .id(id.toString())
        .build();
  }

  /**
   * Builds a DISCORD RemoteUser reference from a Discord id.
   *
   * @param discordId the Discord user id
   * @return the RemoteUser reference
   */
  private RemoteUser remoteUser(String discordId) {
    return RemoteUser.newBuilder()
        .type(RemoteUserType.DISCORD)
        .id(discordId)
        .build();
  }

  /**
   * Loads a post entity and maps it to GraphQL.
   *
   * @param id the post id
   * @return the GraphQL ForumPost
   */
  private ForumPost mapPost(UUID id) {
    ForumPostEntity entity = postService.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
    VoteValue myVote = voteService.myVote(id).orElse(null);
    return postMapper.map(entity, null, myVote);
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
        pagination.getSortDir() == null ? null : pagination.getSortDir().name(),
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
}

package com.sun.icarus.graphql.services;

import com.sun.base.error.MutationException;
import com.sun.base.util.PageRequests;
import com.sun.icarus.codegen.types.ArchiveThreadResponse;
import com.sun.icarus.codegen.types.AttachForumObjectResponse;
import com.sun.icarus.codegen.types.CreatePostInput;
import com.sun.icarus.codegen.types.CreatePostResponse;
import com.sun.icarus.codegen.types.CreateThreadInput;
import com.sun.icarus.codegen.types.CreateThreadResponse;
import com.sun.icarus.codegen.types.DeletePostResponse;
import com.sun.icarus.codegen.types.EditPostResponse;
import com.sun.icarus.codegen.types.ForumPost;
import com.sun.icarus.codegen.types.ForumThread;
import com.sun.icarus.codegen.types.LockThreadResponse;
import com.sun.icarus.codegen.types.PageInfo;
import com.sun.icarus.codegen.types.PagedForumPosts;
import com.sun.icarus.codegen.types.PagedForumThreads;
import com.sun.icarus.codegen.types.PaginationInput;
import com.sun.icarus.codegen.types.RemoveVoteResponse;
import com.sun.icarus.codegen.types.VoteForumResponse;
import com.sun.icarus.codegen.types.ForumObjectReference;
import com.sun.icarus.codegen.types.ForumVoteInput;
import com.sun.icarus.codegen.types.RemoteUser;
import com.sun.icarus.codegen.types.RemoteUserType;
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
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sun.gaia.service.AccountService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
  private final AccountService accountService;

  public IcarusGraphQLService(ForumThreadService threadService, ForumPostService postService,
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
   * Locates a thread by id.
   *
   * @param id the thread id
   * @return the thread, or null
   */
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
   * @param includeHidden whether to include hidden or deleted posts
   * @param pagination the pagination input
   * @return a page of posts
   */
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
   * Returns the caller's vote on a post.
   *
   * @param postId the post id
   * @return the vote value, or null
   */
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
   * Creates a thread attached to a remote object.
   *
   * @param input the create thread input
   * @return the created thread
   */
  @Transactional
  public CreateThreadResponse createThread(CreateThreadInput input) {
    try {
      UUID id = threadService.create(input.getTitle(), input.getRemoteObject());
      ForumThreadEntity thread = threadService.findById(id)
          .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + id));
      logger.info("createThread succeeded for id {}", id);
      return CreateThreadResponse.newBuilder()
          .message("createThread succeeded")
          .thread(threadMapper.map(thread))
          .build();
    } catch (Exception e) {
      logger.error("createThread failed", e);
      throw new MutationException("createThread failed: " + e.getMessage(), e);
    }
  }

  /**
   * Locks a thread.
   *
   * @param id the thread id
   * @return the updated thread
   */
  @Transactional
  public LockThreadResponse lockThread(String id) {
    try {
      UUID threadId = threadService.setStatus(UUID.fromString(id), ThreadStatus.LOCKED);
      ForumThreadEntity thread = threadService.findById(threadId)
          .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));
      logger.info("lockThread succeeded for id {}", threadId);
      return LockThreadResponse.newBuilder()
          .message("lockThread succeeded")
          .thread(threadMapper.map(thread))
          .build();
    } catch (Exception e) {
      logger.error("lockThread failed", e);
      throw new MutationException("lockThread failed: " + e.getMessage(), e);
    }
  }

  /**
   * Archives a thread.
   *
   * @param id the thread id
   * @return the updated thread
   */
  @Transactional
  public ArchiveThreadResponse archiveThread(String id) {
    try {
      UUID threadId = threadService.setStatus(UUID.fromString(id), ThreadStatus.ARCHIVED);
      ForumThreadEntity thread = threadService.findById(threadId)
          .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));
      logger.info("archiveThread succeeded for id {}", threadId);
      return ArchiveThreadResponse.newBuilder()
          .message("archiveThread succeeded")
          .thread(threadMapper.map(thread))
          .build();
    } catch (Exception e) {
      logger.error("archiveThread failed", e);
      throw new MutationException("archiveThread failed: " + e.getMessage(), e);
    }
  }

  /**
   * Adds a post to a thread.
   *
   * @param input the create post input
   * @return the created post
   */
  @Transactional
  public CreatePostResponse createPost(CreatePostInput input) {
    try {
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
    } catch (Exception e) {
      logger.error("createPost failed", e);
      throw new MutationException("createPost failed: " + e.getMessage(), e);
    }
  }

  /**
   * Updates a post's body.
   *
   * @param id the post id
   * @param body the new body
   * @return the updated post
   */
  @Transactional
  public EditPostResponse editPost(String id, String body) {
    try {
      UUID postId = postService.editPost(UUID.fromString(id), body);
      ForumPost post = mapPost(postId);
      logger.info("editPost succeeded for id {}", postId);
      return EditPostResponse.newBuilder()
          .message("editPost succeeded")
          .post(post)
          .build();
    } catch (Exception e) {
      logger.error("editPost failed", e);
      throw new MutationException("editPost failed: " + e.getMessage(), e);
    }
  }

  /**
   * Soft-deletes a post.
   *
   * @param id the post id
   * @return the deleted post id
   */
  @Transactional
  public DeletePostResponse deletePost(String id) {
    try {
      UUID postId = postService.deletePost(UUID.fromString(id));
      logger.info("deletePost succeeded for id {}", postId);
      return DeletePostResponse.newBuilder()
          .message("deletePost succeeded")
          .id(postId.toString())
          .build();
    } catch (Exception e) {
      logger.error("deletePost failed", e);
      throw new MutationException("deletePost failed: " + e.getMessage(), e);
    }
  }

  /**
   * Casts a vote on a post.
   *
   * @param input the vote input
   * @return the voted post
   */
  @Transactional
  public VoteForumResponse vote(ForumVoteInput input) {
    try {
      UUID postId = voteService.vote(UUID.fromString(input.getPostId()), input.getValue());
      ForumPost post = mapPost(postId);
      logger.info("vote succeeded for id {}", postId);
      return VoteForumResponse.newBuilder()
          .message("vote succeeded")
          .post(post)
          .build();
    } catch (Exception e) {
      logger.error("vote failed", e);
      throw new MutationException("vote failed: " + e.getMessage(), e);
    }
  }

  /**
   * Removes the caller's vote on a post.
   *
   * @param postId the post id
   * @return the updated post
   */
  @Transactional
  public RemoveVoteResponse removeVote(String postId) {
    try {
      UUID id = voteService.removeVote(UUID.fromString(postId));
      ForumPost post = mapPost(id);
      logger.info("removeVote succeeded for id {}", id);
      return RemoveVoteResponse.newBuilder()
          .message("removeVote succeeded")
          .post(post)
          .build();
    } catch (Exception e) {
      logger.error("removeVote failed", e);
      throw new MutationException("removeVote failed: " + e.getMessage(), e);
    }
  }

  /**
   * Attaches a remote object id to a thread.
   *
   * @param source the thread id
   * @param target the remote object id
   * @return the thread id
   */
  @Transactional
  public AttachForumObjectResponse attachObject(String source, String target) {
    try {
      UUID id = threadService.attach(UUID.fromString(source), target);
      logger.info("attachObject succeeded for id {}", id);
      return AttachForumObjectResponse.newBuilder()
          .message("attachObject succeeded")
          .id(id.toString())
          .build();
    } catch (Exception e) {
      logger.error("attachObject failed", e);
      throw new MutationException("attachObject failed: " + e.getMessage(), e);
    }
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

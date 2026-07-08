package com.sun.icarus.service;

import com.sun.base.service.BaseService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.icarus.model.ForumPostEntity;
import com.sun.icarus.model.enums.PostStatus;
import com.sun.icarus.repository.ForumPostRepository;
import com.sun.icarus.repository.ForumVoteRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for forum posts.
 */
@Service
@Transactional
public class ForumPostService extends BaseService<ForumPostEntity> {

  private final ForumPostRepository postRepository;
  private final ForumVoteRepository voteRepository;

  public ForumPostService(ForumPostRepository repository, ForumVoteRepository voteRepository) {
    super(repository);
    this.postRepository = repository;
    this.voteRepository = voteRepository;
  }

  /**
   * Lists posts in a thread.
   *
   * @param threadId the thread id
   * @param pageable the page request
   * @return a page of posts
   */
  public Page<ForumPostEntity> listForThread(UUID threadId, Pageable pageable) {
    return postRepository.findByThreadId(threadId, pageable);
  }

  /**
   * Adds a post, optionally replying to a parent.
   *
   * @param threadId the thread id
   * @param parentId an optional parent post id
   * @param body the markdown body
   * @return the new post id
   */
  public UUID addPost(UUID threadId, UUID parentId, String body) {
    requireUser();
    if (body == null || body.isBlank()) {
      throw new IllegalArgumentException("Invalid post");
    }
    ForumPostEntity post = new ForumPostEntity();
    post.setThreadId(threadId);
    post.setParentId(parentId);
    post.setBody(body);
    return postRepository.save(post).getId();
  }

  /**
   * Updates a post's body (author only).
   *
   * @param id the post id
   * @param body the new body
   * @return the post id
   */
  public UUID editPost(UUID id, String body) {
    UUID userId = requireUser();
    ForumPostEntity post = postRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
    if (!post.getCreatedBy().equals(userId)) {
      throw new IllegalArgumentException("Not the author");
    }
    post.setBody(body);
    return postRepository.save(post).getId();
  }

  /**
   * Soft-deletes a post (author only) and removes its votes.
   *
   * @param id the post id
   * @return the post id
   */
  public UUID deletePost(UUID id) {
    UUID userId = requireUser();
    ForumPostEntity post = postRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
    if (!post.getCreatedBy().equals(userId)) {
      throw new IllegalArgumentException("Not the author");
    }
    post.setStatus(PostStatus.DELETED);
    voteRepository.deleteByPostId(id);
    return postRepository.save(post).getId();
  }

  private UUID requireUser() {
    UUID id = UserContextHolder.getUserId();
    if (id == null) {
      throw new IllegalArgumentException("Authentication required");
    }
    return id;
  }
}

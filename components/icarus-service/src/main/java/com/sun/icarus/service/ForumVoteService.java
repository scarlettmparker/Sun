package com.sun.icarus.service;

import com.sun.gaia.service.UserContextHolder;
import com.sun.icarus.model.ForumPostEntity;
import com.sun.icarus.model.ForumVoteEntity;
import com.sun.icarus.model.enums.VoteValue;
import com.sun.icarus.repository.ForumPostRepository;
import com.sun.icarus.repository.ForumVoteRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for casting, toggling, and removing votes on forum posts.
 */
@Service
@Transactional
public class ForumVoteService {

  private final ForumVoteRepository voteRepository;
  private final ForumPostRepository postRepository;

  public ForumVoteService(ForumVoteRepository voteRepository, ForumPostRepository postRepository) {
    this.voteRepository = voteRepository;
    this.postRepository = postRepository;
  }

  /**
   * Casts a vote, toggling off on a repeat and flipping on a change.
   *
   * @param postId the post id
   * @param value up or down
   * @return the post id
   */
  public UUID vote(UUID postId, VoteValue value) {
    UUID accountId = requireUser();
    ForumPostEntity post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
    Optional<ForumVoteEntity> existing = voteRepository.findByAccountIdAndPostId(accountId, postId);

    if (existing.isPresent()) {
      ForumVoteEntity vote = existing.get();
      if (vote.getValue() == value) {
        adjust(post, vote.getValue(), -1);
        voteRepository.delete(vote);
      } else {
        adjust(post, vote.getValue(), -1);
        adjust(post, value, 1);
        vote.setValue(value);
        voteRepository.save(vote);
      }
    } else {
      ForumVoteEntity vote = new ForumVoteEntity();
      vote.setAccountId(accountId);
      vote.setPostId(postId);
      vote.setValue(value);
      voteRepository.save(vote);
      adjust(post, value, 1);
    }
    postRepository.save(post);
    return postId;
  }

  /**
   * Removes the caller's vote on a post.
   *
   * @param postId the post id
   * @return the post id
   */
  public UUID removeVote(UUID postId) {
    UUID accountId = requireUser();
    ForumVoteEntity vote = voteRepository.findByAccountIdAndPostId(accountId, postId)
        .orElseThrow(() -> new IllegalArgumentException("No vote to remove"));
    ForumPostEntity post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
    adjust(post, vote.getValue(), -1);
    voteRepository.delete(vote);
    postRepository.save(post);
    return postId;
  }

  /**
   * Returns the caller's vote on a post, or empty.
   *
   * @param postId the post id
   * @return the vote value, or empty
   */
  public Optional<VoteValue> myVote(UUID postId) {
    UUID accountId = UserContextHolder.getUserId();
    if (accountId == null) {
      return Optional.empty();
    }
    return voteRepository.findByAccountIdAndPostId(accountId, postId).map(ForumVoteEntity::getValue);
  }

  /**
   * Returns the caller's votes for a batch of posts.
   *
   * @param postIds the post ids
   * @return a map of post id to the caller's vote value
   */
  public java.util.Map<UUID, VoteValue> myVotes(java.util.Collection<UUID> postIds) {
    UUID accountId = UserContextHolder.getUserId();
    if (accountId == null || postIds == null || postIds.isEmpty()) {
      return java.util.Map.of();
    }
    return voteRepository.findByAccountIdAndPostIdIn(accountId, postIds).stream()
        .collect(java.util.stream.Collectors.toMap(ForumVoteEntity::getPostId, ForumVoteEntity::getValue));
  }

  private void adjust(ForumPostEntity post, VoteValue value, int delta) {
    if (value == VoteValue.UP) {
      post.setUpvotes(post.getUpvotes() + delta);
    } else {
      post.setDownvotes(post.getDownvotes() + delta);
    }
  }

  private UUID requireUser() {
    UUID id = UserContextHolder.getUserId();
    if (id == null) {
      throw new IllegalArgumentException("Authentication required");
    }
    return id;
  }
}

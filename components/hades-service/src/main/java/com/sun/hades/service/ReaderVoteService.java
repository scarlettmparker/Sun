package com.sun.hades.service;

import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.ReaderCommentEntity;
import com.sun.hades.model.ReaderVoteEntity;
import com.sun.hades.model.enums.ReaderStatus;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import com.sun.hades.repository.ReaderAnnotationRepository;
import com.sun.hades.repository.ReaderCommentRepository;
import com.sun.hades.repository.ReaderVoteRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for casting, toggling, and removing votes, and applying the hide rule.
 */
@Service
@Transactional("hadesTransactionManager")
public class ReaderVoteService {

  private final ReaderVoteRepository voteRepository;
  private final ReaderAnnotationRepository annotationRepository;
  private final ReaderCommentRepository commentRepository;
  private final int hideThreshold;

  public ReaderVoteService(ReaderVoteRepository voteRepository,
      ReaderAnnotationRepository annotationRepository,
      ReaderCommentRepository commentRepository,
      @Value("${hades.hide-threshold:-3}") int hideThreshold) {
    this.voteRepository = voteRepository;
    this.annotationRepository = annotationRepository;
    this.commentRepository = commentRepository;
    this.hideThreshold = hideThreshold;
  }

  /**
   * Casts a vote, toggling off on a repeat and flipping on a change.
   *
   * @param targetType whether the target is an annotation or comment
   * @param targetId the target id
   * @param value up or down
   * @return the target id
   */
  public UUID vote(ReaderVoteTarget targetType, UUID targetId, VoteValue value) {
    UUID accountId = requireUser();
    Optional<ReaderVoteEntity> existing =
        voteRepository.findByAccountIdAndTargetTypeAndTargetId(accountId, targetType, targetId);

    if (existing.isPresent()) {
      ReaderVoteEntity vote = existing.get();
      if (vote.getValue() == value) {
        adjust(targetType, targetId, value, c -> c - 1);
        voteRepository.delete(vote);
      } else {
        adjust(targetType, targetId, vote.getValue(), c -> c - 1);
        adjust(targetType, targetId, value, c -> c + 1);
        vote.setValue(value);
        voteRepository.save(vote);
      }
    } else {
      ReaderVoteEntity vote = new ReaderVoteEntity();
      vote.setAccountId(accountId);
      vote.setTargetType(targetType);
      vote.setTargetId(targetId);
      vote.setValue(value);
      voteRepository.save(vote);
      adjust(targetType, targetId, value, c -> c + 1);
    }
    return targetId;
  }

  /**
   * Removes the caller's vote on a target.
   *
   * @param targetType whether the target is an annotation or comment
   * @param targetId the target id
   * @return the target id
   */
  public UUID removeVote(ReaderVoteTarget targetType, UUID targetId) {
    UUID accountId = requireUser();
    ReaderVoteEntity vote = voteRepository
        .findByAccountIdAndTargetTypeAndTargetId(accountId, targetType, targetId)
        .orElseThrow(() -> new IllegalArgumentException("No vote to remove"));
    adjust(targetType, targetId, vote.getValue(), c -> c - 1);
    voteRepository.delete(vote);
    return targetId;
  }

  /**
   * Returns the caller's vote on a target, or empty.
   *
   * @param targetType whether the target is an annotation or comment
   * @param targetId the target id
   * @return the vote value, or empty
   */
  public Optional<VoteValue> myVote(ReaderVoteTarget targetType, UUID targetId) {
    UUID accountId = UserContextHolder.getUserId();
    if (accountId == null) {
      return Optional.empty();
    }
    return voteRepository
        .findByAccountIdAndTargetTypeAndTargetId(accountId, targetType, targetId)
        .map(ReaderVoteEntity::getValue);
  }

  private void adjust(
      ReaderVoteTarget targetType, UUID targetId, VoteValue value, IntUnaryOperator op) {
    if (targetType == ReaderVoteTarget.ANNOTATION) {
      ReaderAnnotationEntity target = annotationRepository.findById(targetId)
          .orElseThrow(() -> new IllegalArgumentException("Annotation not found: " + targetId));
      if (value == VoteValue.UP) {
        target.setUpvotes(op.applyAsInt(target.getUpvotes()));
      } else {
        target.setDownvotes(op.applyAsInt(target.getDownvotes()));
      }
      target.setStatus(hideStatus(target.getUpvotes() - target.getDownvotes()));
      annotationRepository.save(target);
    } else {
      ReaderCommentEntity target = commentRepository.findById(targetId)
          .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + targetId));
      if (value == VoteValue.UP) {
        target.setUpvotes(op.applyAsInt(target.getUpvotes()));
      } else {
        target.setDownvotes(op.applyAsInt(target.getDownvotes()));
      }
      target.setStatus(hideStatus(target.getUpvotes() - target.getDownvotes()));
      commentRepository.save(target);
    }
  }

  private ReaderStatus hideStatus(int netScore) {
    return netScore <= hideThreshold ? ReaderStatus.HIDDEN : ReaderStatus.ACTIVE;
  }

  private UUID requireUser() {
    UUID id = UserContextHolder.getUserId();
    if (id == null) {
      throw new IllegalArgumentException("Authentication required");
    }
    return id;
  }
}

package com.sun.hades.service;

import com.sun.base.service.BaseService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.ReaderCommentEntity;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.repository.ReaderCommentRepository;
import com.sun.hades.repository.ReaderVoteRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for annotation comments.
 */
@Service
@Transactional("hadesTransactionManager")
public class ReaderCommentService extends BaseService<ReaderCommentEntity> {

  private final ReaderCommentRepository commentRepository;
  private final ReaderVoteRepository voteRepository;

  public ReaderCommentService(ReaderCommentRepository repository,
      ReaderVoteRepository voteRepository) {
    super(repository);
    this.commentRepository = repository;
    this.voteRepository = voteRepository;
  }

  /**
   * Lists comments for an annotation.
   *
   * @param annotationId the annotation id
   * @param pageable the page request
   * @return a page of comments
   */
  public Page<ReaderCommentEntity> listForAnnotation(UUID annotationId, Pageable pageable) {
    return commentRepository.findByAnnotationId(annotationId, pageable);
  }

  /**
   * Adds a comment, optionally replying to a parent.
   *
   * @param annotationId the annotation id
   * @param parentId an optional parent comment id
   * @param body the markdown body
   * @return the new comment id
   */
  public UUID addComment(UUID annotationId, UUID parentId, String body) {
    requireUser();
    if (body == null || body.isBlank()) {
      throw new IllegalArgumentException("Invalid comment");
    }
    ReaderCommentEntity comment = new ReaderCommentEntity();
    comment.setAnnotationId(annotationId);
    comment.setParentId(parentId);
    comment.setBody(body);
    return commentRepository.save(comment).getId();
  }

  /**
   * Updates a comment's body (author only).
   *
   * @param id the comment id
   * @param body the new body
   * @return the comment id
   */
  public UUID editComment(UUID id, String body) {
    UUID userId = requireUser();
    ReaderCommentEntity comment = commentRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + id));
    if (!comment.getCreatedBy().equals(userId)) {
      throw new IllegalArgumentException("Not the author");
    }
    comment.setBody(body);
    return commentRepository.save(comment).getId();
  }

  /**
   * Deletes a comment (author only) and its votes.
   *
   * @param id the comment id
   */
  public void deleteComment(UUID id) {
    UUID userId = requireUser();
    ReaderCommentEntity comment = commentRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + id));
    if (!comment.getCreatedBy().equals(userId)) {
      throw new IllegalArgumentException("Not the author");
    }
    voteRepository.deleteByTargetTypeAndTargetId(ReaderVoteTarget.COMMENT, id);
    commentRepository.deleteById(id);
  }

  private UUID requireUser() {
    UUID id = UserContextHolder.getUserId();
    if (id == null) {
      throw new IllegalArgumentException("Authentication required");
    }
    return id;
  }
}

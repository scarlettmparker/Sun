package com.sun.hades.model;

import com.sun.base.model.BaseEntity;
import com.sun.hades.model.enums.ReaderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A threaded reply on an annotation.
 */
@Entity
@Table(name = "reader_comments")
public class ReaderCommentEntity extends BaseEntity {

  @Column(name = "annotation_id", nullable = false)
  private UUID annotationId;

  @Column(name = "parent_id")
  private UUID parentId;

  @Column(name = "body", nullable = false, columnDefinition = "text")
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReaderStatus status = ReaderStatus.ACTIVE;

  @Column(name = "upvotes", nullable = false)
  private int upvotes;

  @Column(name = "downvotes", nullable = false)
  private int downvotes;

  public UUID getAnnotationId() {
    return annotationId;
  }

  public void setAnnotationId(UUID annotationId) {
    this.annotationId = annotationId;
  }

  public UUID getParentId() {
    return parentId;
  }

  public void setParentId(UUID parentId) {
    this.parentId = parentId;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public ReaderStatus getStatus() {
    return status;
  }

  public void setStatus(ReaderStatus status) {
    this.status = status;
  }

  public int getUpvotes() {
    return upvotes;
  }

  public void setUpvotes(int upvotes) {
    this.upvotes = upvotes;
  }

  public int getDownvotes() {
    return downvotes;
  }

  public void setDownvotes(int downvotes) {
    this.downvotes = downvotes;
  }
}

package com.sun.icarus.model;

import com.sun.base.model.BaseEntity;
import com.sun.icarus.model.enums.PostStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A post in a discussion thread.
 */
@Entity
@Table(name = "icarus_forum_posts")
public class ForumPostEntity extends BaseEntity {

  @Column(name = "thread_id", nullable = false)
  private UUID threadId;

  @Column(name = "parent_id")
  private UUID parentId;

  @Column(name = "body", nullable = false, columnDefinition = "text")
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PostStatus status = PostStatus.ACTIVE;

  @Column(name = "upvotes", nullable = false)
  private int upvotes;

  @Column(name = "downvotes", nullable = false)
  private int downvotes;

  public UUID getThreadId() {
    return threadId;
  }

  public void setThreadId(UUID threadId) {
    this.threadId = threadId;
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

  public PostStatus getStatus() {
    return status;
  }

  public void setStatus(PostStatus status) {
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

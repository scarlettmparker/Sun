package com.sun.icarus.model;

import com.sun.base.model.BaseEntity;
import com.sun.icarus.model.enums.VoteValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * A single account's up/down vote on a forum post.
 */
@Entity
@Table(
    name = "forum_votes",
    uniqueConstraints = @UniqueConstraint(
        name = "forum_vote_unique",
        columnNames = {"account_id", "post_id"}))
public class ForumVoteEntity extends BaseEntity {

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "post_id", nullable = false)
  private UUID postId;

  @Enumerated(EnumType.STRING)
  @Column(name = "value", nullable = false)
  private VoteValue value;

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public UUID getPostId() {
    return postId;
  }

  public void setPostId(UUID postId) {
    this.postId = postId;
  }

  public VoteValue getValue() {
    return value;
  }

  public void setValue(VoteValue value) {
    this.value = value;
  }
}

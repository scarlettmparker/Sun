package com.sun.hades.model;

import com.sun.base.model.BaseEntity;
import com.sun.hades.model.enums.ReaderVoteTarget;
import com.sun.hades.model.enums.VoteValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * A single account's up/down vote on an annotation or comment.
 */
@Entity
@Table(
    name = "hades_reader_votes",
    uniqueConstraints = @UniqueConstraint(
        name = "reader_vote_unique",
        columnNames = {"account_id", "target_type", "target_id"}))
public class ReaderVoteEntity extends BaseEntity {

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false)
  private ReaderVoteTarget targetType;

  @Column(name = "target_id", nullable = false)
  private UUID targetId;

  @Enumerated(EnumType.STRING)
  @Column(name = "value", nullable = false)
  private VoteValue value;

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public ReaderVoteTarget getTargetType() {
    return targetType;
  }

  public void setTargetType(ReaderVoteTarget targetType) {
    this.targetType = targetType;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public void setTargetId(UUID targetId) {
    this.targetId = targetId;
  }

  public VoteValue getValue() {
    return value;
  }

  public void setValue(VoteValue value) {
    this.value = value;
  }
}

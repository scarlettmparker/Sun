package com.sun.hades.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks that a user has viewed a text.
 */
@Entity
@Table(
    name = "hades_text_views",
    uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "text_id"}))
public class TextViewEntity extends BaseEntity {

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "text_id", nullable = false)
  private UUID textId;

  @Column(name = "viewed_at", nullable = false)
  private LocalDateTime viewedAt = LocalDateTime.now();

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public UUID getTextId() {
    return textId;
  }

  public void setTextId(UUID textId) {
    this.textId = textId;
  }

  public LocalDateTime getViewedAt() {
    return viewedAt;
  }

  public void setViewedAt(LocalDateTime viewedAt) {
    this.viewedAt = viewedAt;
  }
}

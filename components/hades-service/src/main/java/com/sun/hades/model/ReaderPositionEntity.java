package com.sun.hades.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * A character range on a text, shared by its annotations and kept until none remain.
 */
@Entity
@Table(
    name = "reader_positions",
    uniqueConstraints = @UniqueConstraint(
        name = "reader_position_range_unique",
        columnNames = {"text_id", "start_offset", "end_offset"}))
public class ReaderPositionEntity extends BaseEntity {

  @Column(name = "text_id", nullable = false)
  private UUID textId;

  @Column(name = "start_offset", nullable = false)
  private int startOffset;

  @Column(name = "end_offset", nullable = false)
  private int endOffset;

  public UUID getTextId() {
    return textId;
  }

  public void setTextId(UUID textId) {
    this.textId = textId;
  }

  public int getStartOffset() {
    return startOffset;
  }

  public void setStartOffset(int startOffset) {
    this.startOffset = startOffset;
  }

  public int getEndOffset() {
    return endOffset;
  }

  public void setEndOffset(int endOffset) {
    this.endOffset = endOffset;
  }
}

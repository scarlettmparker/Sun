package com.sun.echo.model;

import com.sun.base.model.BaseEntity;
import com.sun.echo.model.enums.ItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Join entity linking a checklist entry to one of its items, carrying the
 * per-entry runtime status of that item and an ordering position.
 */
@Entity
@Table(name = "echo_checklist_entry_items")
public class ChecklistEntryItemEntity extends BaseEntity {

  @Column(name = "entry_id", nullable = false)
  private UUID entryId;

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ItemStatus status = ItemStatus.NOT_STARTED;

  @Column(name = "position", nullable = false)
  private int position;

  public UUID getEntryId() {
    return entryId;
  }

  public void setEntryId(UUID entryId) {
    this.entryId = entryId;
  }

  public UUID getItemId() {
    return itemId;
  }

  public void setItemId(UUID itemId) {
    this.itemId = itemId;
  }

  public ItemStatus getStatus() {
    return status;
  }

  public void setStatus(ItemStatus status) {
    this.status = status;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }
}

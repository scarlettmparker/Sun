package com.sun.echo.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Join entity linking a checklist template to one of its items, with an
 * ordering position. Template items carry no runtime status (status lives on
 * the entry-item relationship once a checklist is created from a template).
 */
@Entity
@Table(name = "echo_checklist_template_items")
public class ChecklistTemplateItemEntity extends BaseEntity {

  @Column(name = "template_id", nullable = false)
  private UUID templateId;

  @Column(name = "item_id", nullable = false)
  private UUID itemId;

  @Column(name = "position", nullable = false)
  private int position;

  public UUID getTemplateId() {
    return templateId;
  }

  public void setTemplateId(UUID templateId) {
    this.templateId = templateId;
  }

  public UUID getItemId() {
    return itemId;
  }

  public void setItemId(UUID itemId) {
    this.itemId = itemId;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }
}

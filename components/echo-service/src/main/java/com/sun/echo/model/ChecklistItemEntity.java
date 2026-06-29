package com.sun.echo.model;

import com.sun.base.model.BaseEntity;
import com.sun.echo.model.enums.LifecycleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "checklist_items")
public class ChecklistItemEntity extends BaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "category_id")
  private UUID categoryId;

  @Enumerated(EnumType.STRING)
  @Column(name = "lifecycle_status", nullable = false)
  private LifecycleStatus lifecycleStatus = LifecycleStatus.ACTIVE;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public UUID getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(UUID categoryId) {
    this.categoryId = categoryId;
  }

  public LifecycleStatus getLifecycleStatus() {
    return lifecycleStatus;
  }

  public void setLifecycleStatus(LifecycleStatus lifecycleStatus) {
    this.lifecycleStatus = lifecycleStatus;
  }
}

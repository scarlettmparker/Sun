package com.sun.echo.model;

import com.sun.base.model.BaseEntity;
import com.sun.echo.model.enums.ChecklistStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_entries")
public class ChecklistEntryEntity extends BaseEntity {

  @Column(name = "name")
  private String name;

  @Column(name = "due_at")
  private LocalDateTime dueAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ChecklistStatus status = ChecklistStatus.ACTIVE;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LocalDateTime getDueAt() {
    return dueAt;
  }

  public void setDueAt(LocalDateTime dueAt) {
    this.dueAt = dueAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public ChecklistStatus getStatus() {
    return status;
  }

  public void setStatus(ChecklistStatus status) {
    this.status = status;
  }
}

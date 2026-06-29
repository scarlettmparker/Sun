package com.sun.echo.model;

import com.sun.base.model.BaseEntity;
import com.sun.echo.model.enums.ChecklistStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "checklist_templates")
public class ChecklistTemplateEntity extends BaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ChecklistStatus status = ChecklistStatus.ACTIVE;

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

  public ChecklistStatus getStatus() {
    return status;
  }

  public void setStatus(ChecklistStatus status) {
    this.status = status;
  }
}

package com.sun.medusa.model;

import com.sun.base.model.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "medusa_board_layouts")
public class BoardLayoutEntity extends BaseEntity {

  @Column(name = "slug", nullable = false, unique = true)
  private String slug;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Type(JsonBinaryType.class)
  @Column(name = "placements", columnDefinition = "jsonb", nullable = false)
  private List<Object> placements;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "is_system", nullable = false)
  private boolean system;

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

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

  public List<Object> getPlacements() {
    return placements;
  }

  public void setPlacements(List<Object> placements) {
    this.placements = placements;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UUID createdBy) {
    this.createdBy = createdBy;
  }

  public boolean isSystem() {
    return system;
  }

  public void setSystem(boolean system) {
    this.system = system;
  }
}
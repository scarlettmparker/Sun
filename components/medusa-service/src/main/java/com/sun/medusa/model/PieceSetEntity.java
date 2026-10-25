package com.sun.medusa.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "medusa_piece_sets")
public class PieceSetEntity extends BaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "starting_fen", nullable = false, columnDefinition = "TEXT")
  private String startingFen;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "is_system", nullable = false)
  private boolean system;

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

  public String getStartingFen() {
    return startingFen;
  }

  public void setStartingFen(String startingFen) {
    this.startingFen = startingFen;
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
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
@Table(name = "medusa_piece_definitions")
public class PieceDefinitionEntity extends BaseEntity {

  @Column(name = "slug", nullable = false, unique = true)
  private String slug;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "is_king", nullable = false)
  private boolean king;

  @Type(JsonBinaryType.class)
  @Column(name = "promotion_targets", columnDefinition = "jsonb", nullable = false)
  private List<String> promotionTargets;

  @Type(JsonBinaryType.class)
  @Column(name = "promotion_ranks", columnDefinition = "jsonb", nullable = false)
  private List<Integer> promotionRanks;

  @Type(JsonBinaryType.class)
  @Column(name = "move_states", columnDefinition = "jsonb", nullable = false)
  private List<Object> moveStates;

  @Column(name = "state_transition", nullable = false)
  private String stateTransition;

  @Column(name = "edge_reset", nullable = false)
  private boolean edgeReset;

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

  public boolean isKing() {
    return king;
  }

  public void setKing(boolean king) {
    this.king = king;
  }

  public List<String> getPromotionTargets() {
    return promotionTargets;
  }

  public void setPromotionTargets(List<String> promotionTargets) {
    this.promotionTargets = promotionTargets;
  }

  public List<Integer> getPromotionRanks() {
    return promotionRanks;
  }

  public void setPromotionRanks(List<Integer> promotionRanks) {
    this.promotionRanks = promotionRanks;
  }

  public List<Object> getMoveStates() {
    return moveStates;
  }

  public void setMoveStates(List<Object> moveStates) {
    this.moveStates = moveStates;
  }

  public String getStateTransition() {
    return stateTransition;
  }

  public void setStateTransition(String stateTransition) {
    this.stateTransition = stateTransition;
  }

  public boolean isEdgeReset() {
    return edgeReset;
  }

  public void setEdgeReset(boolean edgeReset) {
    this.edgeReset = edgeReset;
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
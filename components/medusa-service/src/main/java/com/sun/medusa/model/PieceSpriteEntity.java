package com.sun.medusa.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "medusa_piece_sprites")
public class PieceSpriteEntity extends BaseEntity {

  @Column(name = "piece_definition_id", nullable = false, unique = true)
  private UUID pieceDefinitionId;

  @Column(name = "head_key", nullable = false)
  private String headKey;

  @Column(name = "body_key", nullable = false)
  private String bodyKey;

  @Column(name = "foot_key", nullable = false)
  private String footKey;

  public UUID getPieceDefinitionId() {
    return pieceDefinitionId;
  }

  public void setPieceDefinitionId(UUID pieceDefinitionId) {
    this.pieceDefinitionId = pieceDefinitionId;
  }

  public String getHeadKey() {
    return headKey;
  }

  public void setHeadKey(String headKey) {
    this.headKey = headKey;
  }

  public String getBodyKey() {
    return bodyKey;
  }

  public void setBodyKey(String bodyKey) {
    this.bodyKey = bodyKey;
  }

  public String getFootKey() {
    return footKey;
  }

  public void setFootKey(String footKey) {
    this.footKey = footKey;
  }
}
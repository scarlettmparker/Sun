package com.sun.medusa.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "medusa_piece_set_members")
public class PieceSetMemberEntity {

  @EmbeddedId
  private PieceSetMemberId id;

  @jakarta.persistence.Column(name = "piece_definition_id", nullable = false)
  private UUID pieceDefinitionId;

  public PieceSetMemberEntity() {}

  public PieceSetMemberEntity(UUID pieceSetId, String slot, UUID pieceDefinitionId) {
    this.id = new PieceSetMemberId(pieceSetId, slot);
    this.pieceDefinitionId = pieceDefinitionId;
  }

  public PieceSetMemberId getId() {
    return id;
  }

  public void setId(PieceSetMemberId id) {
    this.id = id;
  }

  public UUID getPieceDefinitionId() {
    return pieceDefinitionId;
  }

  public void setPieceDefinitionId(UUID pieceDefinitionId) {
    this.pieceDefinitionId = pieceDefinitionId;
  }
}
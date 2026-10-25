package com.sun.medusa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PieceSetMemberId implements Serializable {

  @Column(name = "piece_set_id")
  private UUID pieceSetId;

  @Column(name = "slot")
  private String slot;

  public PieceSetMemberId() {}

  public PieceSetMemberId(UUID pieceSetId, String slot) {
    this.pieceSetId = pieceSetId;
    this.slot = slot;
  }

  public UUID getPieceSetId() {
    return pieceSetId;
  }

  public void setPieceSetId(UUID pieceSetId) {
    this.pieceSetId = pieceSetId;
  }

  public String getSlot() {
    return slot;
  }

  public void setSlot(String slot) {
    this.slot = slot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PieceSetMemberId)) return false;
    PieceSetMemberId that = (PieceSetMemberId) o;
    return Objects.equals(pieceSetId, that.pieceSetId) && Objects.equals(slot, that.slot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pieceSetId, slot);
  }
}
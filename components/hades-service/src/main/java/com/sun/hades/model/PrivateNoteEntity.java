package com.sun.hades.model;

import com.sun.base.model.BaseEntity;
import com.sun.hades.model.enums.PrivateNoteVisibility;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Type;

/**
 * A per-user private note anchored to a text range.
 */
@Entity
@Table(name = "hades_private_notes")
public class PrivateNoteEntity extends BaseEntity {

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "text_id", nullable = false)
  private UUID textId;

  @Column(name = "start_offset", nullable = false)
  private int startOffset;

  @Column(name = "end_offset", nullable = false)
  private int endOffset;

  @Column(name = "body", nullable = false, columnDefinition = "text")
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(name = "visibility", nullable = false)
  private PrivateNoteVisibility visibility = PrivateNoteVisibility.PRIVATE;

  @Type(JsonBinaryType.class)
  @Column(name = "remote_object", columnDefinition = "jsonb")
  private List<String> remoteObject;

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public UUID getTextId() {
    return textId;
  }

  public void setTextId(UUID textId) {
    this.textId = textId;
  }

  public int getStartOffset() {
    return startOffset;
  }

  public void setStartOffset(int startOffset) {
    this.startOffset = startOffset;
  }

  public int getEndOffset() {
    return endOffset;
  }

  public void setEndOffset(int endOffset) {
    this.endOffset = endOffset;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public PrivateNoteVisibility getVisibility() {
    return visibility;
  }

  public void setVisibility(PrivateNoteVisibility visibility) {
    this.visibility = visibility;
  }

  public List<String> getRemoteObject() {
    return remoteObject;
  }

  public void setRemoteObject(List<String> remoteObject) {
    this.remoteObject = remoteObject;
  }
}

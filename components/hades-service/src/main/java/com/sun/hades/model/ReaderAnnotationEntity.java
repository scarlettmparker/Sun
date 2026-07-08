package com.sun.hades.model;

import com.sun.base.model.BaseEntity;
import com.sun.hades.model.enums.ReaderStatus;
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
 * A markdown explanation a user wrote for a position on a text.
 */
@Entity
@Table(name = "hades_reader_annotations")
public class ReaderAnnotationEntity extends BaseEntity {

  @Column(name = "position_id", nullable = false)
  private UUID positionId;

  @Column(name = "body", nullable = false, columnDefinition = "text")
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReaderStatus status = ReaderStatus.ACTIVE;

  @Column(name = "upvotes", nullable = false)
  private int upvotes;

  @Column(name = "downvotes", nullable = false)
  private int downvotes;

  @Type(JsonBinaryType.class)
  @Column(name = "remote_object", columnDefinition = "jsonb")
  private List<String> remoteObject;

  public UUID getPositionId() {
    return positionId;
  }

  public void setPositionId(UUID positionId) {
    this.positionId = positionId;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public ReaderStatus getStatus() {
    return status;
  }

  public void setStatus(ReaderStatus status) {
    this.status = status;
  }

  public int getUpvotes() {
    return upvotes;
  }

  public void setUpvotes(int upvotes) {
    this.upvotes = upvotes;
  }

  public int getDownvotes() {
    return downvotes;
  }

  public void setDownvotes(int downvotes) {
    this.downvotes = downvotes;
  }

  public List<String> getRemoteObject() {
    return remoteObject;
  }

  public void setRemoteObject(List<String> remoteObject) {
    this.remoteObject = remoteObject;
  }
}

package com.sun.dionysus.graphql.models;

import jakarta.persistence.*;
import com.sun.base.model.BaseEntity;
import java.time.LocalDateTime;

/**
 * Entity representing metadata for a key (file/folder) in S3 storage.
 * Tracks name, description, status (active/archived), and audit timestamps.
 */
@Entity
@Table(name = "key_detail")
public class KeyDetail extends BaseEntity {

  @Column
  private LocalDateTime archivedAt;

  @Column(nullable = false)
  private String bucket;

  @Column(nullable = false)
  private String keyPath;

  @Column
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.ACTIVE;

  public LocalDateTime getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(LocalDateTime archivedAt) {
    this.archivedAt = archivedAt;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getKeyPath() {
    return keyPath;
  }

  public void setKeyPath(String keyPath) {
    this.keyPath = keyPath;
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

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
package com.sun.dionysus.model;

import jakarta.persistence.*;
import com.sun.base.model.BaseEntity;
import com.sun.dionysus.model.enums.Status;
import java.time.LocalDateTime;

/**
 * Entity representing metadata for a key (file/folder) in S3 storage.
 * Tracks name, description, status (active/archived), and audit timestamps.
 */
@Entity
@Table(name = "dionysus_key_detail")
public class KeyDetailEntity extends BaseEntity {

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

  @Column
  private String contentType;

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

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
}
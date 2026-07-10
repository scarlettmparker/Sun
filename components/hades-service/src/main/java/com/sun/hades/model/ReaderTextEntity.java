package com.sun.hades.model;

import com.sun.base.model.BaseEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.model.enums.ReaderTextStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A foreign-language text that users annotate.
 */
@Entity
@Table(name = "hades_reader_texts")
public class ReaderTextEntity extends BaseEntity {

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "language", nullable = false)
  private String language;

  @Enumerated(EnumType.STRING)
  @Column(name = "level", nullable = false)
  private CefrLevel level;

  @Column(name = "owner_id")
  private UUID ownerId;

  @Column(name = "source_id")
  private UUID sourceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReaderTextStatus status = ReaderTextStatus.ACTIVE;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public CefrLevel getLevel() {
    return level;
  }

  public void setLevel(CefrLevel level) {
    this.level = level;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public UUID getSourceId() {
    return sourceId;
  }

  public void setSourceId(UUID sourceId) {
    this.sourceId = sourceId;
  }

  public ReaderTextStatus getStatus() {
    return status;
  }

  public void setStatus(ReaderTextStatus status) {
    this.status = status;
  }
}

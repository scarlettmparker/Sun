package com.sun.hades.model;

import com.sun.base.model.BaseEntity;
import com.sun.hades.model.enums.CefrLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * Immutable snapshot of a text before an edit.
 */
@Entity
@Table(
    name = "hades_text_versions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"text_id", "version"}))
public class TextVersionEntity extends BaseEntity {

  @Column(name = "text_id", nullable = false)
  private UUID textId;

  @Column(name = "version", nullable = false)
  private int version;

  @Column(name = "title", nullable = false, columnDefinition = "text")
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "level", nullable = false)
  private CefrLevel level;

  @Column(name = "language", nullable = false)
  private String language;

  @Column(name = "edited_by")
  private UUID editedBy;

  public UUID getTextId() {
    return textId;
  }

  public void setTextId(UUID textId) {
    this.textId = textId;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

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

  public CefrLevel getLevel() {
    return level;
  }

  public void setLevel(CefrLevel level) {
    this.level = level;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public UUID getEditedBy() {
    return editedBy;
  }

  public void setEditedBy(UUID editedBy) {
    this.editedBy = editedBy;
  }
}

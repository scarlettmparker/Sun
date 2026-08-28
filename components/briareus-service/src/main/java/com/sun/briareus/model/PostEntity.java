package com.sun.briareus.model;

import com.sun.base.model.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "briareus_posts")
public class PostEntity extends BaseEntity {

  @Column(name = "title")
  private String title;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private List<String> tags;

  @Type(JsonBinaryType.class)
  @Column(name = "remote_object", columnDefinition = "jsonb")
  private List<String> remoteObject;

  @ManyToOne
  @JoinColumn(name = "type_id")
  private BlogPostTypeEntity type;

  @Column(name = "language")
  private String language;

  @Column(name = "parent_id")
  private UUID parentId;

  // Getters and setters
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

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public List<String> getRemoteObject() {
    return remoteObject;
  }

  public void setRemoteObject(List<String> remoteObject) {
    this.remoteObject = remoteObject;
  }

  public BlogPostTypeEntity getType() {
    return type;
  }

  public void setType(BlogPostTypeEntity type) {
    this.type = type;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public UUID getParentId() {
    return parentId;
  }

  public void setParentId(UUID parentId) {
    this.parentId = parentId;
  }
}
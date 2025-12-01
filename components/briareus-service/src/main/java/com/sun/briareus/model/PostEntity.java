package com.sun.briareus.model;

import com.sun.base.model.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.util.List;

@Entity
@Table(name = "posts")
public class PostEntity extends BaseEntity {
  
  @Column(name = "title")
  private String title;

  @Column(name = "content")
  private String content;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private List<String> tags;

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
}
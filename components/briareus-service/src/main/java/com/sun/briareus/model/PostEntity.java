package com.sun.briareus.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "posts")
public class PostEntity extends BaseEntity {
  
  @Column(name = "title")
  private String title;

  @Column(name = "content")
  private String content;

  @Column(name = "tags")
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
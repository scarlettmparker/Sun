package com.sun.cerberus.model;

import com.sun.base.model.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.util.List;

@Entity
@Table(name = "gallery_items")
public class GalleryItemEntity extends BaseEntity {

  @Column(name = "title")
  private String title;

  @Column(name = "description")
  private String description;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @Column(name = "image_path")
  private String imagePath;

  @Type(JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private List<String> foreignObject;

  // Getters and setters
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public List<String> getForeignObject() {
    return foreignObject;
  }

  public void setForeignObject(List<String> foreignObject) {
    this.foreignObject = foreignObject;
  }
}
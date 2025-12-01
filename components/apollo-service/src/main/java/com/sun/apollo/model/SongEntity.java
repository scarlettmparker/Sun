package com.sun.apollo.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "songs")
public class SongEntity extends BaseEntity {

  @Column(name = "name")
  private String name;

  @Column(name = "file_path")
  private String filePath;

  @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<StemEntity> stems;

  // Getters and setters
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public List<StemEntity> getStems() {
    return stems;
  }

  public void setStems(List<StemEntity> stems) {
    this.stems = stems;
  }
}
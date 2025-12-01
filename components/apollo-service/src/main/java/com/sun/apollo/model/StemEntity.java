package com.sun.apollo.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "stems")
public class StemEntity extends BaseEntity {

  @Column(name = "file_path")
  private String filePath;

  @Column(name = "name")
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id")
  private SongEntity song;

  // Getters and setters
  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public SongEntity getSong() {
    return song;
  }

  public void setSong(SongEntity song) {
    this.song = song;
  }
}
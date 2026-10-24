package com.sun.dionysus.model;

import com.sun.base.model.BaseEntity;
import com.sun.dionysus.model.enums.SubtitleSource;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.Type;

/**
 * A subtitle object attached to a video key.
 */
@Entity
@Table(name = "dionysus_subtitles")
public class SubtitleEntity extends BaseEntity {

  @Column(nullable = false)
  private String bucket;

  @Column(nullable = false)
  private String keyPath;

  @Column(nullable = false)
  private String subtitleKey;

  @Column(nullable = false)
  private String language;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubtitleSource source = SubtitleSource.LOCAL;

  @Column
  private Integer openSubtitlesFileId;

  @Type(JsonBinaryType.class)
  @Column(name = "remote_object", columnDefinition = "jsonb")
  private List<String> remoteObject;

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

  public String getSubtitleKey() {
    return subtitleKey;
  }

  public void setSubtitleKey(String subtitleKey) {
    this.subtitleKey = subtitleKey;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public SubtitleSource getSource() {
    return source;
  }

  public void setSource(SubtitleSource source) {
    this.source = source;
  }

  public Integer getOpenSubtitlesFileId() {
    return openSubtitlesFileId;
  }

  public void setOpenSubtitlesFileId(Integer openSubtitlesFileId) {
    this.openSubtitlesFileId = openSubtitlesFileId;
  }

  public List<String> getRemoteObject() {
    return remoteObject;
  }

  public void setRemoteObject(List<String> remoteObject) {
    this.remoteObject = remoteObject;
  }
}

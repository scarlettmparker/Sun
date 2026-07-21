package com.sun.dionysus.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Torrent metadata captured at add time: info hash, trackers, file layout, and
 * the magnet or .torrent file it came from.
 */
@Entity
@Table(name = "dionysus_magnet_detail")
public class MagnetDetailEntity extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String infoHash;

  @Column(length = 16)
  private String infoHashVersion;

  @Column(nullable = false)
  private String displayName;

  @Column(length = 2048)
  private String sourceUri;

  @Column(nullable = false)
  private long totalSize;

  @Column(length = 2048)
  private String comment;

  @Column(length = 512)
  private String createdBy;

  @Column
  private Long pieceLength;

  @Column
  private Integer pieceCount;

  @Column(nullable = false)
  private boolean isPrivate;

  @ElementCollection
  @CollectionTable(
      name = "dionysus_magnet_tracker",
      joinColumns = @JoinColumn(name = "magnet_id"))
  @Column(name = "tracker", length = 512)
  private List<String> trackers = new ArrayList<>();

  @OneToMany(mappedBy = "magnetDetail", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("indexInTorrent ASC")
  private List<TorrentFileEntity> files = new ArrayList<>();

  public String getInfoHash() {
    return infoHash;
  }

  public void setInfoHash(String infoHash) {
    this.infoHash = infoHash;
  }

  public String getInfoHashVersion() {
    return infoHashVersion;
  }

  public void setInfoHashVersion(String infoHashVersion) {
    this.infoHashVersion = infoHashVersion;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getSourceUri() {
    return sourceUri;
  }

  public void setSourceUri(String sourceUri) {
    this.sourceUri = sourceUri;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(long totalSize) {
    this.totalSize = totalSize;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getCreatedByTorrent() {
    return createdBy;
  }

  public void setCreatedByTorrent(String createdBy) {
    this.createdBy = createdBy;
  }

  public Long getPieceLength() {
    return pieceLength;
  }

  public void setPieceLength(Long pieceLength) {
    this.pieceLength = pieceLength;
  }

  public Integer getPieceCount() {
    return pieceCount;
  }

  public void setPieceCount(Integer pieceCount) {
    this.pieceCount = pieceCount;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public void setPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
  }

  public List<String> getTrackers() {
    return trackers;
  }

  public void setTrackers(List<String> trackers) {
    this.trackers = trackers;
  }

  public List<TorrentFileEntity> getFiles() {
    return files;
  }

  public void setFiles(List<TorrentFileEntity> files) {
    this.files = files;
  }
}

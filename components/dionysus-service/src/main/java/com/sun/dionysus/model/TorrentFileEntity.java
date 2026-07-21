package com.sun.dionysus.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A single file within a torrent's file list, as declared by its metadata.
 */
@Entity
@Table(name = "dionysus_torrent_file")
public class TorrentFileEntity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "magnet_id", nullable = false)
  private MagnetDetailEntity magnetDetail;

  @Column(nullable = false)
  private int indexInTorrent;

  @Column(nullable = false)
  private String path;

  @Column(nullable = false)
  private long size;

  public MagnetDetailEntity getMagnetDetail() {
    return magnetDetail;
  }

  public void setMagnetDetail(MagnetDetailEntity magnetDetail) {
    this.magnetDetail = magnetDetail;
  }

  public int getIndexInTorrent() {
    return indexInTorrent;
  }

  public void setIndexInTorrent(int indexInTorrent) {
    this.indexInTorrent = indexInTorrent;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }
}

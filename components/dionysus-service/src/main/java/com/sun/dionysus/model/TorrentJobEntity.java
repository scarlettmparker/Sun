package com.sun.dionysus.model;

import com.sun.base.model.BaseEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * A torrent being downloaded into a bucket key, keeping the progress and on-disk
 * scratch state needed to resume after a restart.
 */
@Entity
@Table(
    name = "dionysus_torrent_job",
    indexes = {
      @Index(name = "idx_job_status", columnList = "status"),
      @Index(name = "idx_job_bucket_path", columnList = "bucket,target_key_path")
    })
public class TorrentJobEntity extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String infoHash;

  @Column(nullable = false)
  private String bucket;

  @Column(nullable = false)
  private String targetKeyPath;

  @Column(nullable = false, length = 16)
  private String sourceType;

  @OneToOne(fetch = FetchType.LAZY, optional = false, orphanRemoval = true)
  @JoinColumn(name = "magnet_detail_id", nullable = false)
  private MagnetDetailEntity magnetDetail;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "key_detail_id")
  private KeyDetailEntity keyDetail;

  @Column(nullable = false)
  private String scratchPath;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TorrentStatus status = TorrentStatus.QUEUED;

  @Column(nullable = false)
  private long totalBytes;

  @Column(nullable = false)
  private long downloadedBytes;

  @Column(nullable = false)
  private long uploadedBytes;

  @Column(nullable = false)
  private double progress;

  @Column
  private Integer downloadRateBps;

  @Column
  private Integer uploadRateBps;

  @Column
  private Integer peersConnected;

  @Column
  private Integer seedsConnected;

  @Column
  private Long etaSeconds;

  @Column
  private Integer priority;

  @Column(length = 4000)
  private String errorMessage;

  @Column
  private LocalDateTime completedAt;

  @Column
  private LocalDateTime pausedAt;

  public String getInfoHash() {
    return infoHash;
  }

  public void setInfoHash(String infoHash) {
    this.infoHash = infoHash;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getTargetKeyPath() {
    return targetKeyPath;
  }

  public void setTargetKeyPath(String targetKeyPath) {
    this.targetKeyPath = targetKeyPath;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public MagnetDetailEntity getMagnetDetail() {
    return magnetDetail;
  }

  public void setMagnetDetail(MagnetDetailEntity magnetDetail) {
    this.magnetDetail = magnetDetail;
  }

  public KeyDetailEntity getKeyDetail() {
    return keyDetail;
  }

  public void setKeyDetail(KeyDetailEntity keyDetail) {
    this.keyDetail = keyDetail;
  }

  public String getScratchPath() {
    return scratchPath;
  }

  public void setScratchPath(String scratchPath) {
    this.scratchPath = scratchPath;
  }

  public TorrentStatus getStatus() {
    return status;
  }

  public void setStatus(TorrentStatus status) {
    this.status = status;
  }

  public long getTotalBytes() {
    return totalBytes;
  }

  public void setTotalBytes(long totalBytes) {
    this.totalBytes = totalBytes;
  }

  public long getDownloadedBytes() {
    return downloadedBytes;
  }

  public void setDownloadedBytes(long downloadedBytes) {
    this.downloadedBytes = downloadedBytes;
  }

  public long getUploadedBytes() {
    return uploadedBytes;
  }

  public void setUploadedBytes(long uploadedBytes) {
    this.uploadedBytes = uploadedBytes;
  }

  public double getProgress() {
    return progress;
  }

  public void setProgress(double progress) {
    this.progress = progress;
  }

  public Integer getDownloadRateBps() {
    return downloadRateBps;
  }

  public void setDownloadRateBps(Integer downloadRateBps) {
    this.downloadRateBps = downloadRateBps;
  }

  public Integer getUploadRateBps() {
    return uploadRateBps;
  }

  public void setUploadRateBps(Integer uploadRateBps) {
    this.uploadRateBps = uploadRateBps;
  }

  public Integer getPeersConnected() {
    return peersConnected;
  }

  public void setPeersConnected(Integer peersConnected) {
    this.peersConnected = peersConnected;
  }

  public Integer getSeedsConnected() {
    return seedsConnected;
  }

  public void setSeedsConnected(Integer seedsConnected) {
    this.seedsConnected = seedsConnected;
  }

  public Long getEtaSeconds() {
    return etaSeconds;
  }

  public void setEtaSeconds(Long etaSeconds) {
    this.etaSeconds = etaSeconds;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public LocalDateTime getPausedAt() {
    return pausedAt;
  }

  public void setPausedAt(LocalDateTime pausedAt) {
    this.pausedAt = pausedAt;
  }
}

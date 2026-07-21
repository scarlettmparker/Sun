package com.sun.dionysus.model.enums;

/**
 * Lifecycle state of a torrent download job.
 */
public enum TorrentStatus {
  QUEUED,
  METADATA,
  DOWNLOADING,
  PAUSED,
  UPLOADING,
  COMPLETED,
  FAILED,
  CANCELLED
}

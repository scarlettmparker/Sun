package com.sun.dionysus.service.torrent;

import com.sun.base.service.BaseService;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.repository.TorrentJobEntityRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for TorrentJob entities: lookup, the one-active-job-per-key rule, and
 * the prefix-scoped listing that feeds in-progress keys into the bucket view.
 */
@Service
@Transactional
public class TorrentJobService extends BaseService<TorrentJobEntity> {

  /**
   * Statuses that still represent an unfinished download and so should appear in
   * the bucket listing as an in-progress key.
   */
  public static final Set<TorrentStatus> VISIBLE_STATUSES =
      EnumSet.of(
          TorrentStatus.QUEUED,
          TorrentStatus.METADATA,
          TorrentStatus.DOWNLOADING,
          TorrentStatus.PAUSED,
          TorrentStatus.UPLOADING,
          TorrentStatus.FAILED);

  /**
   * Statuses that block a new job for the same key path.
   */
  public static final Set<TorrentStatus> ACTIVE_STATUSES =
      EnumSet.of(
          TorrentStatus.QUEUED,
          TorrentStatus.METADATA,
          TorrentStatus.DOWNLOADING,
          TorrentStatus.PAUSED,
          TorrentStatus.UPLOADING);

  /**
   * Statuses that can be resumed after a restart.
   */
  public static final Set<TorrentStatus> RESUMABLE_STATUSES =
      EnumSet.of(
          TorrentStatus.QUEUED,
          TorrentStatus.METADATA,
          TorrentStatus.DOWNLOADING,
          TorrentStatus.PAUSED,
          TorrentStatus.UPLOADING);

  private final TorrentJobEntityRepository jobRepository;

  public TorrentJobService(TorrentJobEntityRepository repository) {
    super(repository);
    this.jobRepository = repository;
  }

  /**
   * Finds a job by its torrent info hash.
   */
  public Optional<TorrentJobEntity> findByInfoHash(String infoHash) {
    return jobRepository.findByInfoHash(infoHash);
  }

  /**
   * Finds a job by the bucket and key path it targets.
   */
  public Optional<TorrentJobEntity> findByBucketAndTargetKeyPath(String bucket, String targetKeyPath) {
    return jobRepository.findByBucketAndTargetKeyPath(bucket, targetKeyPath);
  }

  /**
   * Returns whether an unfinished job already targets the given key, used to
   * enforce the one-active-job-per-key rule before inserting.
   */
  public boolean hasActiveAt(String bucket, String targetKeyPath) {
    return jobRepository.findByBucketAndStatusIn(bucket, ACTIVE_STATUSES).stream()
        .anyMatch(j -> targetKeyPath.equals(j.getTargetKeyPath()));
  }

  /**
   * Returns all jobs that should be re-added to the torrent session on startup.
   */
  public List<TorrentJobEntity> findResumable() {
    return jobRepository.findByStatusIn(RESUMABLE_STATUSES);
  }

  /**
   * Returns all non-terminal jobs whose target sits directly under the given
   * prefix, mirroring the one-level-deep delimiter semantics of the S3 listing.
   */
  public List<TorrentJobEntity> findVisibleInBucketUnderPrefix(String bucket, String prefix) {
    String normalized = prefix == null ? "" : prefix;
    return jobRepository.findByBucketAndStatusIn(bucket, VISIBLE_STATUSES).stream()
        .filter(job -> isDirectlyUnder(job.getTargetKeyPath(), normalized))
        .toList();
  }

  /**
   * Marks the given jobs paused and records the pause time, used during a
   * graceful shutdown so they resume on the next startup.
   */
  public void markAllPaused(List<UUID> jobIds) {
    for (UUID id : jobIds) {
      jobRepository
          .findById(id)
          .ifPresent(
              job -> {
                job.setStatus(TorrentStatus.PAUSED);
                job.setPausedAt(LocalDateTime.now());
                jobRepository.save(job);
              });
    }
  }

  /**
   * True when the target key path is one level deep beneath the prefix.
   */
  private boolean isDirectlyUnder(String targetKeyPath, String prefix) {
    if (!targetKeyPath.startsWith(prefix)) {
      return false;
    }
    String rest = targetKeyPath.substring(prefix.length());
    String trimmed = rest.endsWith("/") ? rest.substring(0, rest.length() - 1) : rest;
    return !trimmed.isEmpty() && !trimmed.contains("/");
  }
}

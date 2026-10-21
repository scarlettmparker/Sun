package com.sun.dionysus.torrent;

import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.codegen.types.TorrentJob;
import com.sun.dionysus.graphql.mappers.KeyEntryMapper;
import com.sun.dionysus.graphql.mappers.TorrentJobMapper;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads and maps torrent jobs inside a transaction so lazy magnet metadata
 * resolves reliably with open-in-view disabled.
 */
@Component
public class TorrentJobViewService {

  private final TorrentJobService jobService;
  private final TorrentJobMapper jobMapper;
  private final KeyEntryMapper keyEntryMapper;

  public TorrentJobViewService(
      TorrentJobService jobService, TorrentJobMapper jobMapper, KeyEntryMapper keyEntryMapper) {
    this.jobService = jobService;
    this.jobMapper = jobMapper;
    this.keyEntryMapper = keyEntryMapper;
  }

  /**
   * Loads and maps a job by id.
   */
  @Transactional(readOnly = true)
  public Optional<TorrentJob> byId(UUID jobId) {
    return jobService.findById(jobId).map(jobMapper::map);
  }

  /**
   * Loads, filters, and maps all jobs in one transaction.
   */
  @Transactional(readOnly = true)
  public List<TorrentJob> list(String bucket, TorrentStatus status) {
    return jobService.findAll().stream()
        .filter(j -> bucket == null || bucket.equals(j.getBucket()))
        .filter(j -> status == null || status.equals(j.getStatus()))
        .map(jobMapper::map)
        .toList();
  }

  /**
   * Folds in-progress jobs under the prefix into the key listing inside a
   * transaction, returning the synthetic entries created for key paths with
   * no S3 object yet.
   */
  @Transactional(readOnly = true)
  public List<KeyEntry> mergeInProgressJobs(String bucket, String prefix, Map<String, KeyEntry> entriesByKey) {
    List<KeyEntry> synthetic = new ArrayList<>();
    for (TorrentJobEntity job : jobService.findVisibleInBucketUnderPrefix(bucket, prefix)) {
      KeyEntry existing = entriesByKey.get(job.getTargetKeyPath());
      if (existing != null) {
        keyEntryMapper.mergeTorrentJob(existing, job);
      } else {
        KeyEntry entry = keyEntryMapper.mapTorrentJob(job);
        synthetic.add(entry);
        entriesByKey.put(entry.getKey(), entry);
      }
    }
    return synthetic;
  }
}

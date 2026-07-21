package com.sun.dionysus.torrent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.libtorrent4j.TorrentHandle;
import org.springframework.stereotype.Component;

/**
 * In-memory bridge between libtorrent handles and torrent job ids, keyed by the
 * handle's save path so alerts can find their job without a DB hit per update.
 */
@Component
public class TorrentJobRegistry {

  private final ConcurrentHashMap<String, UUID> savePathToJobId = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, TorrentHandle> jobIdToHandle = new ConcurrentHashMap<>();

  /**
   * Records the job id for a given save path and stashes the handle.
   */
  public void register(UUID jobId, String savePath, TorrentHandle handle) {
    savePathToJobId.put(savePath, jobId);
    if (handle != null) {
      jobIdToHandle.put(jobId, handle);
    }
  }

  /**
   * Resolves a job id from a handle's save path.
   */
  public Optional<UUID> findJobId(String savePath) {
    return Optional.ofNullable(savePath).map(savePathToJobId::get);
  }

  /**
   * Returns the live handle for a job, if any.
   */
  public Optional<TorrentHandle> findHandle(UUID jobId) {
    return Optional.ofNullable(jobIdToHandle.get(jobId));
  }

  /**
   * All job ids currently registered with a live handle.
   */
  public Set<UUID> jobIds() {
    return jobIdToHandle.keySet();
  }

  /**
   * Drops a job's mappings.
   */
  public void forget(UUID jobId, String savePath) {
    if (savePath != null) {
      savePathToJobId.remove(savePath);
    }
    jobIdToHandle.remove(jobId);
  }
}

package com.sun.dionysus.torrent;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fails torrent jobs stuck fetching metadata, which happens when a magnet has
 * no reachable peers.
 */
@Component
public class TorrentMetadataWatchdog {

  private static final Logger logger = LoggerFactory.getLogger(TorrentMetadataWatchdog.class);
  private static final Duration METADATA_TIMEOUT = Duration.ofMinutes(30);

  @Autowired private TorrentJobService jobService;

  /**
   * Runs each minute, failing any job that has lingered in the metadata state.
   */
  @Scheduled(fixedDelay = 60_000L)
  public void timeoutStaleMetadataJobs() {
    LocalDateTime cutoff = LocalDateTime.now().minus(METADATA_TIMEOUT);
    for (TorrentJobEntity job : jobService.findAll()) {
      if (job.getStatus() == TorrentStatus.METADATA
          && job.getLastUpdatedAt() != null
          && job.getLastUpdatedAt().isBefore(cutoff)) {
        job.setStatus(TorrentStatus.FAILED);
        job.setErrorMessage("timed out waiting for torrent metadata");
        jobService.save(job);
        logger.warn("Timed out metadata fetch for torrent job {}", job.getId());
      }
    }
  }
}

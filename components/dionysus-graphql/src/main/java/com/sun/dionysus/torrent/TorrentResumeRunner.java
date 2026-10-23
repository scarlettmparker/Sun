package com.sun.dionysus.torrent;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Reconciles transmission state and resumes unfinished jobs after a restart.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TorrentResumeRunner implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(TorrentResumeRunner.class);

  @Autowired private TorrentJobService jobService;
  @Autowired private TorrentClientService client;
  @Autowired private TransmissionGateway transmissionGateway;

  /**
   * Removes stale transmission entries and resumes unfinished jobs.
   *
   * @param args the application arguments.
   */
  @Override
  public void run(ApplicationArguments args) {
    List<TorrentJobEntity> resumable = jobService.findResumable();
    Set<String> keep = hashes(resumable);
    Set<String> stale =
        jobService.findAll().stream()
            .filter(
                job ->
                    job.getStatus() == TorrentStatus.CANCELLED
                        || job.getStatus() == TorrentStatus.COMPLETED)
            .map(TorrentJobEntity::getInfoHash)
            .filter(hash -> hash != null && !hash.isBlank())
            .map(hash -> hash.toLowerCase(Locale.ROOT))
            .filter(hash -> !keep.contains(hash))
            .collect(Collectors.toSet());

    if (!stale.isEmpty()) {
      logger.info("Removing {} stale transmission torrent(s)", stale.size());
      transmissionGateway.removeTorrentsByInfoHash(stale, true);
    }

    if (resumable.isEmpty()) {
      return;
    }

    logger.info("Resuming {} torrent job(s) after restart", resumable.size());
    for (TorrentJobEntity job : resumable) {
      try {
        client.resumeExistingJob(job.getId());
      } catch (Exception e) {
        logger.warn("Failed to resume torrent job {}", job.getId(), e);
      }
    }
  }

  /**
   * Normalizes the info hashes of a job list.
   *
   * @param jobs the torrent jobs.
   * @return the lowercase info hashes.
   */
  private Set<String> hashes(List<TorrentJobEntity> jobs) {
    return jobs.stream()
        .map(TorrentJobEntity::getInfoHash)
        .filter(hash -> hash != null && !hash.isBlank())
        .map(hash -> hash.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }
}

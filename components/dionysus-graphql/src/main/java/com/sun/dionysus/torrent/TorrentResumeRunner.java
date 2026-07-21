package com.sun.dionysus.torrent;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.service.torrent.TorrentJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Re-adds resumable torrent jobs to the session after a restart, so a download
 * paused by a shutdown picks up where it left off.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TorrentResumeRunner implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(TorrentResumeRunner.class);

  @Autowired private TorrentJobService jobService;
  @Autowired private TorrentClientService client;

  @Override
  public void run(ApplicationArguments args) {
    var resumable = jobService.findResumable();
    if (resumable.isEmpty()) {
      return;
    }
    logger.info("Resuming {} torrent job(s) after restart", resumable.size());
    for (TorrentJobEntity job : resumable) {
      try {
        client.resumeExistingJob(job);
      } catch (Exception e) {
        logger.warn("Failed to resume torrent job {}", job.getId(), e);
      }
    }
  }
}

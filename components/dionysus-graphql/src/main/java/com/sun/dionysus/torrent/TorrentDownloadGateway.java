package com.sun.dionysus.torrent;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.File;
import java.util.UUID;
import org.libtorrent4j.SessionManager;
import org.libtorrent4j.TorrentInfo;
import org.libtorrent4j.swig.torrent_flags_t;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs the blocking libtorrent download on the async executor, marking the job
 * failed if the download throws before any alert can.
 */
@Component
public class TorrentDownloadGateway {

  private static final Logger logger = LoggerFactory.getLogger(TorrentDownloadGateway.class);

  @Autowired private SessionManager session;
  @Autowired private TorrentJobService jobService;

  /**
   * Downloads a magnet on a background thread; returns when the torrent finishes.
   */
  @Async("torrentTaskExecutor")
  public void downloadMagnet(UUID jobId, String magnet, File saveDir) {
    try {
      session.download(magnet, saveDir, new torrent_flags_t());
    } catch (Exception e) {
      logger.error("Magnet download failed for job {}", jobId, e);
      markFailed(jobId, e);
    }
  }

  /**
   * Downloads a parsed .torrent on a background thread; returns when it finishes.
   */
  @Async("torrentTaskExecutor")
  public void downloadTorrentFile(UUID jobId, byte[] torrentBytes, File saveDir) {
    try {
      session.download(new TorrentInfo(torrentBytes), saveDir);
    } catch (Exception e) {
      logger.error(".torrent download failed for job {}", jobId, e);
      markFailed(jobId, e);
    }
  }

  private void markFailed(UUID jobId, Exception error) {
    jobService
        .findById(jobId)
        .ifPresent(
            job -> {
              job.setStatus(TorrentStatus.FAILED);
              job.setErrorMessage(error.getMessage());
              jobService.save(job);
            });
  }
}

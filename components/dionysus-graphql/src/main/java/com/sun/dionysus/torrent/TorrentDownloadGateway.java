package com.sun.dionysus.torrent;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.File;
import java.util.Random;
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
 * failed if the download throws before any alert can. When no native library is
 * available, simulates download progress for development.
 */
@Component
public class TorrentDownloadGateway {

  private static final Logger logger = LoggerFactory.getLogger(TorrentDownloadGateway.class);

  private static final Random RNG = new Random();

  @Autowired private TorrentJobService jobService;

  /**
   * Downloads a magnet on a background thread; returns when the torrent finishes.
   */
  @Async("torrentTaskExecutor")
  public void downloadMagnet(UUID jobId, SessionManager session, String magnet, File saveDir) {
    if (session == null) {
      logger.info("Torrent in dev mode — simulating download for job {}", jobId);
      simulateDownload(jobId, 100_000_000L + RNG.nextInt(400_000_000));
      return;
    }
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
  public void downloadTorrentFile(UUID jobId, SessionManager session, byte[] torrentBytes, File saveDir) {
    if (session == null) {
      logger.info("Torrent in dev mode — simulating download for job {}", jobId);
      long totalBytes = 0L;
      try {
        TorrentInfo info = new TorrentInfo(torrentBytes);
        totalBytes = info.files().totalSize();
      } catch (Exception ignored) {
        totalBytes = 100_000_000L + RNG.nextInt(400_000_000);
      }
      simulateDownload(jobId, totalBytes);
      return;
    }
    try {
      session.download(new TorrentInfo(torrentBytes), saveDir);
    } catch (Exception e) {
      logger.error(".torrent download failed for job {}", jobId, e);
      markFailed(jobId, e);
    }
  }

  private void simulateDownload(UUID jobId, long totalBytes) {
    jobService.findById(jobId).ifPresent(job -> {
      job.setStatus(TorrentStatus.DOWNLOADING);
      job.setTotalBytes(totalBytes);
      jobService.save(job);
    });

    long start = System.currentTimeMillis();
    long simulatedBytes = 0L;
    int rateBps = 2_500_000 + RNG.nextInt(5_000_000); // 2.5–7.5 MB/s

    while (simulatedBytes < totalBytes) {
      try {
        Thread.sleep(1500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }

      long elapsed = (System.currentTimeMillis() - start) / 1000;
      simulatedBytes += rateBps * 1L;
      if (simulatedBytes > totalBytes) simulatedBytes = totalBytes;
      double progress = (double) simulatedBytes / totalBytes;

      long finalSimulatedBytes = simulatedBytes;
      jobService.findById(jobId).ifPresent(job -> {
        job.setDownloadedBytes(finalSimulatedBytes);
        job.setProgress(progress);
        job.setDownloadRateBps(rateBps);
        job.setPeersConnected(3 + RNG.nextInt(10));
        job.setEtaSeconds((long) ((totalBytes - finalSimulatedBytes) / rateBps));
        jobService.save(job);
      });
    }

    jobService.findById(jobId).ifPresent(job -> {
      job.setStatus(TorrentStatus.COMPLETED);
      job.setProgress(1.0);
      job.setDownloadRateBps(0);
      job.setPeersConnected(0);
      job.setEtaSeconds(0L);
      jobService.save(job);
    });
    logger.info("Simulated download completed for job {}", jobId);
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

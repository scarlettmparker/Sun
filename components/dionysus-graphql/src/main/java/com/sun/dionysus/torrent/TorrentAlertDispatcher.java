package com.sun.dionysus.torrent;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.libtorrent4j.AlertListener;
import org.libtorrent4j.TorrentHandle;
import org.libtorrent4j.alerts.AddTorrentAlert;
import org.libtorrent4j.alerts.Alert;
import org.libtorrent4j.alerts.MetadataReceivedAlert;
import org.libtorrent4j.alerts.StateChangedAlert;
import org.libtorrent4j.alerts.TorrentErrorAlert;
import org.libtorrent4j.alerts.TorrentFinishedAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Routes libtorrent alerts to database updates on the alert thread, throttled so
 * a busy download does not flood the database with progress writes.
 */
@Component
public class TorrentAlertDispatcher implements AlertListener {

  private static final Logger logger = LoggerFactory.getLogger(TorrentAlertDispatcher.class);
  private static final long PROGRESS_THROTTLE_MS = 2000L;

  private final TorrentJobService jobService;
  private final TorrentJobRegistry registry;
  private final TorrentCompletionService completionService;
  private final TransactionTemplate transactionTemplate;

  /**
   * Per-job throttle state: last persisted wall-clock millis and integer percent.
   */
  private final ConcurrentHashMap<UUID, long[]> throttle = new ConcurrentHashMap<>();

  public TorrentAlertDispatcher(
      TorrentJobService jobService,
      TorrentJobRegistry registry,
      TorrentCompletionService completionService,
      PlatformTransactionManager transactionManager) {
    this.jobService = jobService;
    this.registry = registry;
    this.completionService = completionService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Override
  public int[] types() {
    // null subscribes to every alert category.
    return null;
  }

  @Override
  public void alert(Alert<?> alert) {
    try {
      if (alert instanceof MetadataReceivedAlert metadata) {
        onMetadata(metadata.handle());
      } else if (alert instanceof AddTorrentAlert add) {
        onAddTorrent(add);
      } else if (alert instanceof TorrentFinishedAlert finished) {
        onFinish(finished.handle());
      } else if (alert instanceof TorrentErrorAlert error) {
        onError(error);
      } else if (alert instanceof StateChangedAlert state) {
        onState(state.handle());
      }
    } catch (Exception e) {
      logger.warn("Failed to handle torrent alert", e);
    }
  }

  /**
   * Records the handle so pause/resume/cancel can find the torrent later.
   */
  private void onAddTorrent(AddTorrentAlert alert) {
    if (alert.error() != null && alert.error().isError()) {
      logger.warn("Add torrent error: {}", alert.error().getMessage());
    }
    TorrentHandle handle = alert.handle();
    withJob(handle, job -> registry.register(job.getId(), job.getScratchPath(), handle));
  }

  /**
   * Fills total size and name from the now-available metadata, then starts downloading.
   */
  private void onMetadata(TorrentHandle handle) {
    withJob(
        handle,
        job -> {
          org.libtorrent4j.TorrentStatus status = handle.status();
          job.setStatus(TorrentStatus.DOWNLOADING);
          job.setTotalBytes(status.totalWanted());
          if (job.getMagnetDetail().getDisplayName() == null && status.name() != null) {
            job.getMagnetDetail().setDisplayName(status.name());
          }
          jobService.save(job);
        });
  }

  /**
   * Hands the finished job to the S3 upload service.
   */
  private void onFinish(TorrentHandle handle) {
    withJob(
        handle,
        job -> {
          job.setStatus(TorrentStatus.UPLOADING);
          jobService.save(job);
          completionService.complete(job.getId());
        });
  }

  /**
   * Marks a job failed with the libtorrent error message.
   */
  private void onError(TorrentErrorAlert alert) {
    withJob(
        alert.handle(),
        job -> {
          job.setStatus(TorrentStatus.FAILED);
          job.setErrorMessage(alert.error() != null ? alert.error().getMessage() : "torrent error");
          jobService.save(job);
        });
  }

  /**
   * Updates live progress and rate fields, throttled to avoid a write per piece.
   */
  private void onState(TorrentHandle handle) {
    withJob(
        handle,
        job -> {
          org.libtorrent4j.TorrentStatus status = handle.status();
          int percent = (int) (status.progress() * 100);
          long[] last = throttle.computeIfAbsent(job.getId(), k -> new long[] {0L, -1L});
          long now = System.currentTimeMillis();
          boolean justFinished =
              status.state() == org.libtorrent4j.TorrentStatus.State.FINISHED;
          if (!justFinished && now - last[0] < PROGRESS_THROTTLE_MS && percent == last[1]) {
            return;
          }
          last[0] = now;
          last[1] = percent;
          job.setProgress(status.progress());
          job.setDownloadedBytes(status.totalWantedDone());
          job.setUploadedBytes(status.totalPayloadUpload());
          job.setDownloadRateBps(status.downloadRate());
          job.setUploadRateBps(status.uploadRate());
          job.setPeersConnected(status.numPeers());
          job.setSeedsConnected(status.numSeeds());
          if (job.getStatus() == TorrentStatus.METADATA) {
            job.setStatus(TorrentStatus.DOWNLOADING);
          }
          jobService.save(job);
        });
  }

  /**
   * Resolves the job for a handle by its save path and runs an action in a transaction.
   */
  private void withJob(TorrentHandle handle, Consumer<TorrentJobEntity> action) {
    if (handle == null || !handle.isValid()) {
      return;
    }
    String savePath = handle.savePath();
    registry
        .findJobId(savePath)
        .ifPresent(
            jobId ->
                transactionTemplate.executeWithoutResult(
                    status -> jobService.findById(jobId).ifPresent(action)));
  }
}

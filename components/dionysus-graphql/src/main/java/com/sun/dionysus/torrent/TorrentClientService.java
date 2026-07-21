package com.sun.dionysus.torrent;

import com.sun.dionysus.model.MagnetDetailEntity;
import com.sun.dionysus.model.TorrentFileEntity;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.MagnetDetailService;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.libtorrent4j.FileStorage;
import org.libtorrent4j.SessionManager;
import org.libtorrent4j.TorrentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

/**
 * The embedded torrent client: owns the libtorrent session, starts and stops it
 * with the application, and exposes add/pause/resume/cancel for jobs.
 */
@Service
@EnableConfigurationProperties(TorrentClientProperties.class)
public class TorrentClientService implements SmartLifecycle {

  private static final Logger logger = LoggerFactory.getLogger(TorrentClientService.class);

  private SessionManager session;

  @Autowired private TorrentAlertDispatcher dispatcher;
  @Autowired private TorrentJobService jobService;
  @Autowired private MagnetDetailService magnetService;
  @Autowired private TorrentJobRegistry registry;
  @Autowired private TorrentDownloadGateway gateway;
  @Autowired private WebTorrentGateway webGateway;
  @Autowired private TorrentClientProperties properties;

  private volatile boolean running = false;
  private File scratchRoot;

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public int getPhase() {
    // High phase so this stops before the datasource tears down.
    return 1_000_000;
  }

  @Override
  public synchronized void start() {
    try {
      session = new SessionManager();
    } catch (LinkageError e) {
      logger.warn("libtorrent4j native library not available — torrent client disabled: {}", e.getMessage());
      return;
    }
    scratchRoot = new File(properties.getScratchDir());
    scratchRoot.mkdirs();
    session.start();
    session.addListener(dispatcher);
    running = true;
    logger.info("Torrent client started, scratch dir {}", scratchRoot.getAbsolutePath());
  }

  @Override
  public synchronized void stop() {
    running = false;
    if (session == null) return;
    for (UUID jobId : registry.jobIds()) {
      registry
          .findHandle(jobId)
          .ifPresent(
              handle -> {
                try {
                  handle.pause();
                } catch (Exception ignored) {
                }
              });
    }
    jobService.markAllPaused(new ArrayList<>(registry.jobIds()));
    session.stop();
    logger.info("Torrent client stopped, paused {} active job(s)", registry.jobIds().size());
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  /**
   * Returns the session manager, or null if the native library was unavailable.
   */
  public SessionManager getSession() {
    return session;
  }

  /**
   * Adds a magnet link as a new download job targeting a key under the parent path.
   */
  public TorrentJobEntity addFromMagnet(String bucket, String parentPath, String magnet) {
    MagnetUri parsed = MagnetUri.parse(magnet);
    String name = parsed.displayName() != null ? parsed.displayName() : parsed.infoHash();
    String targetKeyPath = targetKeyPath(parentPath, name, false);
    guardUnique(bucket, targetKeyPath);

    MagnetDetailEntity magnetDetail = new MagnetDetailEntity();
    magnetDetail.setInfoHash(parsed.infoHash());
    magnetDetail.setDisplayName(name);
    magnetDetail.setSourceUri(magnet);
    magnetDetail.setTotalSize(0L);
    magnetDetail.setTrackers(parsed.trackers());
    magnetDetail.setPrivate(false);

    TorrentJobEntity job = newJob(bucket, targetKeyPath, "MAGNET", parsed.infoHash(), magnetDetail, TorrentStatus.DOWNLOADING, 0L);
    job.setScratchPath("");
    job = jobService.save(job);

    File saveDir = newFile(job.getId());
    job.setScratchPath(saveDir.getAbsolutePath());
    job = jobService.save(job);

    registry.register(job.getId(), job.getScratchPath(), null);
    webGateway.downloadMagnet(job.getId(), magnet, saveDir);
    return job;
  }

  /**
   * Adds a .torrent file (raw bytes) as a new download job.
   */
  public TorrentJobEntity addFromTorrentFile(String bucket, String parentPath, byte[] bytes) {
    TorrentInfo info = new TorrentInfo(bytes);
    FileStorage storage = info.files();
    String name = storage.name();
    boolean multiFile = storage.numFiles() > 1;
    String targetKeyPath = targetKeyPath(parentPath, name, multiFile);
    guardUnique(bucket, targetKeyPath);

    long total = 0L;
    MagnetDetailEntity magnetDetail = new MagnetDetailEntity();
    magnetDetail.setInfoHash(info.infoHash().toString());
    magnetDetail.setDisplayName(name);
    magnetDetail.setSourceUri("magnet:?xt=urn:btih:" + info.infoHash() + "&dn=" + encode(name));
    magnetDetail.setPrivate(false);
    java.util.List<TorrentFileEntity> files = new ArrayList<>();
    for (int index = 0; index < storage.numFiles(); index++) {
      long size = storage.fileSize(index);
      total += size;
      TorrentFileEntity file = new TorrentFileEntity();
      file.setIndexInTorrent(index);
      file.setPath(storage.filePath(index));
      file.setSize(size);
      file.setMagnetDetail(magnetDetail);
      files.add(file);
    }
    magnetDetail.setFiles(files);
    magnetDetail.setTotalSize(total);

    TorrentJobEntity job = newJob(bucket, targetKeyPath, "FILE", info.infoHash().toString(), magnetDetail, TorrentStatus.DOWNLOADING, total);
    job.setScratchPath("");
    job = jobService.save(job);

    File saveDir = newFile(job.getId());
    job.setScratchPath(saveDir.getAbsolutePath());
    job = jobService.save(job);

    registry.register(job.getId(), job.getScratchPath(), null);

    File torrentFile = new File(saveDir, "source.torrent");
    try {
      Files.write(torrentFile.toPath(), bytes);
    } catch (IOException e) {
      logger.error("Failed to write torrent file for job {}", job.getId(), e);
    }
    webGateway.downloadTorrentFile(job.getId(), torrentFile, saveDir);
    return job;
  }

  /**
   * Re-adds an existing job to the session on startup, resuming into its scratch dir.
   */
  public void resumeExistingJob(TorrentJobEntity job) {
    File saveDir = new File(job.getScratchPath());
    if (!saveDir.isDirectory()) {
      job.setStatus(TorrentStatus.FAILED);
      job.setErrorMessage("scratch dir missing on restart");
      jobService.save(job);
      return;
    }
    registry.register(job.getId(), job.getScratchPath(), null);
    job.setStatus(TorrentStatus.DOWNLOADING);
    jobService.save(job);
    String magnet = job.getMagnetDetail().getSourceUri();
    gateway.downloadMagnet(job.getId(), session, magnet, saveDir);
  }

  /**
   * Pauses a running download.
   */
  public void pauseJob(UUID jobId) {
    jobService
        .findById(jobId)
        .ifPresent(
            job -> {
              registry.findHandle(jobId).ifPresent(handle -> {
                try {
                  handle.pause();
                } catch (Exception ignored) {
                }
              });
              job.setStatus(TorrentStatus.PAUSED);
              jobService.save(job);
            });
  }

  /**
   * Resumes a paused download.
   */
  public void resumeJob(UUID jobId) {
    jobService
        .findById(jobId)
        .ifPresent(
            job -> {
              registry.findHandle(jobId).ifPresent(handle -> {
                try {
                  handle.resume();
                } catch (Exception ignored) {
                }
              });
              job.setStatus(TorrentStatus.DOWNLOADING);
              jobService.save(job);
            });
  }

  /**
   * Cancels a job, removes it from the session, and deletes its scratch files.
   */
  public void cancelJob(UUID jobId) {
    jobService
        .findById(jobId)
        .ifPresent(
            job -> {
              if (session != null) {
                registry
                    .findHandle(jobId)
                    .ifPresent(
                        handle -> {
                          try {
                            handle.pause();
                            session.remove(handle);
                          } catch (Exception ignored) {
                          }
                        });
              }
              registry.forget(jobId, job.getScratchPath());
              job.setStatus(TorrentStatus.CANCELLED);
              jobService.save(job);
              deleteQuietly(new File(job.getScratchPath()));
            });
  }

  private void guardUnique(String bucket, String targetKeyPath) {
    for (TorrentJobEntity existing : jobService.findByBucketAndTargetKeyPath(bucket, targetKeyPath)) {
      if (TorrentJobService.ACTIVE_STATUSES.contains(existing.getStatus())) {
        logger.info("Cancelling existing active job {} for {}/{}", existing.getId(), bucket, targetKeyPath);
        jobService.updateStatus(existing.getId(), TorrentStatus.CANCELLED);
        deleteQuietly(new File(existing.getScratchPath()));
      }
    }
  }

  private File newFile(UUID jobId) {
    File saveDir = new File(scratchRoot, jobId.toString());
    saveDir.mkdirs();
    return saveDir;
  }

  private TorrentJobEntity newJob(
      String bucket,
      String targetKeyPath,
      String sourceType,
      String infoHash,
      MagnetDetailEntity magnetDetail,
      TorrentStatus status,
      long totalBytes) {
    TorrentJobEntity job = new TorrentJobEntity();
    job.setBucket(bucket);
    job.setTargetKeyPath(targetKeyPath);
    job.setSourceType(sourceType);
    job.setInfoHash(infoHash);
    job.setMagnetDetail(magnetDetail);
    job.setStatus(status);
    job.setTotalBytes(totalBytes);
    job.setDownloadedBytes(0L);
    job.setUploadedBytes(0L);
    job.setProgress(0.0);
    return job;
  }

  private String targetKeyPath(String parentPath, String name, boolean isFolder) {
    String base = parentPath == null ? "" : parentPath;
    return isFolder ? base + name + "/" : base + name;
  }

  private String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      return value;
    }
  }

  private void deleteQuietly(File file) {
    if (file == null || !file.exists()) {
      return;
    }
    try (var paths = Files.walk(file.toPath())) {
      paths.sorted(java.util.Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
              });
    } catch (IOException ignored) {
    }
  }
}

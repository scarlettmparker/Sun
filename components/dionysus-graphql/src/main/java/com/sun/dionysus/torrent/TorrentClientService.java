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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.libtorrent4j.FileStorage;
import org.libtorrent4j.SessionManager;
import org.libtorrent4j.TorrentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  @Autowired private Aria2Gateway aria2Gateway;
  @Autowired private TransmissionGateway transmissionGateway;
  @Autowired private TorrentClientProperties properties;

  private volatile boolean running = false;
  private File scratchRoot;

  /**
   * Reports whether the client starts with the application.
   *
   * @return true for automatic startup.
   */
  @Override
  public boolean isAutoStartup() {
    return true;
  }

  /**
   * Stops the client after dependent beans have shut down.
   *
   * @return the lifecycle phase.
   */
  @Override
  public int getPhase() {
    return 1_000_000;
  }

  /**
   * Starts the embedded session when its native library is available.
   */
  @Override
  public synchronized void start() {
    scratchRoot = new File(properties.getScratchDir());
    scratchRoot.mkdirs();
    scratchRoot.setWritable(true, false);
    scratchRoot.setReadable(true, false);
    scratchRoot.setExecutable(true, false);
    try {
      session = new SessionManager();
      session.start();
      session.addListener(dispatcher);
      running = true;
      logger.info("Torrent client started, scratch dir {}", scratchRoot.getAbsolutePath());
    } catch (LinkageError e) {
      logger.warn("libtorrent4j native library not available - torrent client disabled: {}", e.getMessage());
    }
  }

  /**
   * Pauses tracked jobs and stops the embedded session.
   */
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

  /**
   * Reports whether the embedded session is running.
   *
   * @return true when the session started.
   */
  @Override
  public boolean isRunning() {
    return running;
  }

  /**
   * Returns the embedded session.
   *
   * @return the session, or null when the native library is unavailable.
   */
  public SessionManager getSession() {
    return session;
  }

  /**
   * Adds a magnet link as a new download job.
   *
   * @param bucket the target bucket.
   * @param parentPath the parent key path.
   * @param magnet the magnet URI.
   * @return the new job.
   */
  public TorrentJobEntity addFromMagnet(String bucket, String parentPath, String magnet) {
    MagnetUri parsed = MagnetUri.parse(magnet);
    String name = parsed.displayName() == null ? parsed.infoHash() : parsed.displayName();
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
    transmissionGateway.downloadMagnet(job.getId(), magnet, saveDir);
    return job;
  }

  /**
   * Adds a .torrent file as a new download job.
   *
   * @param bucket the target bucket.
   * @param parentPath the parent key path.
   * @param bytes the raw .torrent bytes.
   * @return the new job.
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
    List<TorrentFileEntity> files = new ArrayList<>();
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
    transmissionGateway.downloadTorrentFile(job.getId(), torrentFile, saveDir);
    return job;
  }

  /**
   * Re-adds an existing job to the session on startup.
   *
   * @param jobId the torrent job id.
   */
  @Transactional
  public void resumeExistingJob(UUID jobId) {
    TorrentJobEntity job = jobService.findById(jobId)
        .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    File saveDir = resolveScratchDir(job);
    job.setStatus(TorrentStatus.DOWNLOADING);
    job.setErrorMessage(null);
    job.setScratchPath(saveDir.getAbsolutePath());
    jobService.save(job);

    File torrentFile = torrentFileFor(job, saveDir);
    if (torrentFile != null) {
      transmissionGateway.resumeTorrentFile(job.getId(), torrentFile, saveDir);
      return;
    }
    transmissionGateway.resumeMagnet(
        job.getId(), job.getMagnetDetail().getSourceUri(), saveDir);
  }

  /**
   * Chooses the scratch directory to resume into.
   *
   * @param job the torrent job.
   * @return the prepared scratch directory.
   */
  private File resolveScratchDir(TorrentJobEntity job) {
    File root = scratchRoot == null ? new File(properties.getScratchDir()) : scratchRoot;
    File canonical = new File(root, job.getId().toString());
    File stored = scratchFile(job);

    if (stored != null && stored.isDirectory() && TorrentScratch.hasData(stored)) {
      if (needsMigration(stored, root)) {
        transmissionGateway.removeTorrentByInfoHash(job.getInfoHash());
        prepareDir(canonical);
        if (moveTree(stored.toPath(), canonical.toPath())) {
          TorrentScratch.deleteRecursively(stored.toPath());
          logger.info("Moved torrent data for job {} into {}", job.getId(), canonical);
          return canonical;
        }
        logger.warn("Could not fully move torrent data for job {}", job.getId());
        return stored;
      }
      prepareDir(stored);
      return stored;
    }

    prepareDir(canonical);
    return canonical;
  }

  /**
   * Reports whether a stored scratch directory sits under the old /tmp root.
   *
   * @param stored the stored scratch directory.
   * @param root the configured scratch root.
   * @return true when the data should move into the configured root.
   */
  private boolean needsMigration(File stored, File root) {
    try {
      Path storedPath = stored.getCanonicalFile().toPath();
      Path rootPath = root.getCanonicalFile().toPath();
      Path legacyPath = new File("/tmp/dionysus/torrent").getCanonicalFile().toPath();
      return !storedPath.startsWith(rootPath) && storedPath.startsWith(legacyPath);
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Moves a scratch directory tree to a new location.
   *
   * @param from the source directory.
   * @param to the destination directory.
   * @return true when every file reached the destination.
   */
  private boolean moveTree(Path from, Path to) {
    boolean complete = true;
    try (Stream<Path> paths = Files.walk(from)) {
      for (Path source : paths.toList()) {
        Path destination = to.resolve(from.relativize(source).toString());
        if (Files.isDirectory(source)) {
          Files.createDirectories(destination);
        } else {
          Files.createDirectories(destination.getParent());
          complete = moveFile(source, destination) && complete;
        }
      }
    } catch (IOException e) {
      logger.warn("Could not move scratch data from {} to {}: {}", from, to, e.getMessage());
      return false;
    }
    return complete;
  }

  /**
   * Moves one file, falling back to copy and delete across filesystems.
   *
   * @param source the source file.
   * @param destination the destination file.
   * @return true when the file reached the destination.
   */
  private boolean moveFile(Path source, Path destination) {
    try {
      Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException moveFailure) {
      try {
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        Files.delete(source);
        return true;
      } catch (IOException copyFailure) {
        logger.warn("Could not move file {}: {}", source, copyFailure.getMessage());
        return false;
      }
    }
  }

  /**
   * Locates or restores the .torrent file for a file-based job.
   *
   * @param job the torrent job.
   * @param saveDir the chosen scratch directory.
   * @return the .torrent file, or null for magnet jobs.
   */
  private File torrentFileFor(TorrentJobEntity job, File saveDir) {
    if (!"FILE".equalsIgnoreCase(job.getSourceType())) {
      return null;
    }
    File primary = new File(saveDir, "source.torrent");
    if (primary.isFile()) {
      return primary;
    }
    File stored = scratchFile(job);
    if (stored == null) {
      return null;
    }
    File legacy = new File(stored, "source.torrent");
    if (!legacy.isFile()) {
      return null;
    }
    try {
      Files.copy(legacy.toPath(), primary.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return primary;
    } catch (IOException e) {
      logger.warn("Could not restore torrent file for job {}: {}", job.getId(), e.getMessage());
      return null;
    }
  }

  /**
   * Pauses a running download.
   *
   * @param jobId the torrent job id.
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
   *
   * @param jobId the torrent job id.
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
   * Cancels a job, removing its transmission entry and scratch directory.
   *
   * @param jobId the torrent job id.
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
              if (!jobService.hasOtherResumableInfoHash(jobId, job.getInfoHash())) {
                transmissionGateway.removeTransmission(jobId);
              }
              if (!jobService.hasOtherResumableScratchPath(jobId, job.getScratchPath())) {
                TorrentScratch.delete(scratchRoot, scratchFile(job));
              }
              registry.forget(jobId, job.getScratchPath());
              job.setStatus(TorrentStatus.CANCELLED);
              jobService.save(job);
            });
  }

  /**
   * Cancels any active job targeting the same key before a new one starts.
   *
   * @param bucket the target bucket.
   * @param targetKeyPath the target key path.
   */
  private void guardUnique(String bucket, String targetKeyPath) {
    for (TorrentJobEntity existing : jobService.findByBucketAndTargetKeyPath(bucket, targetKeyPath)) {
      if (TorrentJobService.ACTIVE_STATUSES.contains(existing.getStatus())) {
        logger.info("Cancelling existing active job {} for {}/{}", existing.getId(), bucket, targetKeyPath);
        cancelJob(existing.getId());
      }
    }
  }

  /**
   * Resolves a job's scratch directory.
   *
   * @param job the torrent job.
   * @return the scratch directory, or null when the path is blank.
   */
  private File scratchFile(TorrentJobEntity job) {
    String path = job.getScratchPath();
    return path == null || path.isBlank() ? null : new File(path);
  }

  /**
   * Creates the scratch directory for a job.
   *
   * @param jobId the torrent job id.
   * @return the job scratch directory.
   */
  private File newFile(UUID jobId) {
    File saveDir = new File(scratchRoot, jobId.toString());
    prepareDir(saveDir);
    return saveDir;
  }

  /**
   * Creates a scratch directory with the permissions transmission needs.
   *
   * @param dir the directory to prepare.
   */
  private void prepareDir(File dir) {
    dir.mkdirs();
    dir.setWritable(true, false);
    dir.setReadable(true, false);
    dir.setExecutable(true, false);
  }

  /**
   * Builds a torrent job entity.
   *
   * @param bucket the target bucket.
   * @param targetKeyPath the target key path.
   * @param sourceType the torrent source type.
   * @param infoHash the torrent info hash.
   * @param magnetDetail the torrent metadata.
   * @param status the initial status.
   * @param totalBytes the total byte count.
   * @return the new job entity.
   */
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

  /**
   * Builds the target key path for a torrent.
   *
   * @param parentPath the parent key path.
   * @param name the torrent name.
   * @param isFolder whether the torrent is multi-file.
   * @return the target key path.
   */
  private String targetKeyPath(String parentPath, String name, boolean isFolder) {
    String base = parentPath == null ? "" : parentPath;
    return isFolder ? base + name + "/" : base + name;
  }

  /**
   * URL-encodes a value.
   *
   * @param value the source value.
   * @return the encoded value.
   */
  private String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      return value;
    }
  }

}

package com.sun.dionysus.torrent;

import com.sun.dionysus.model.KeyDetailEntity;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.KeyDetailService;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Uploads a finished torrent's files into the bucket, activates the matching
 * key detail, and clears the scratch directory.
 */
@Service
public class TorrentCompletionService {

  private static final Logger logger = LoggerFactory.getLogger(TorrentCompletionService.class);

  @Autowired private TorrentJobService jobService;
  @Autowired private KeyDetailService keyDetailService;
  @Autowired private S3Client s3Client;
  @Autowired private TorrentJobRegistry registry;

  /**
   * Uploads the completed download into S3 and finalises the job.
   */
  @Async("torrentTaskExecutor")
  public void complete(UUID jobId) {
    TorrentJobEntity job =
        jobService.findById(jobId).orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));
    if (job.getStatus() == TorrentStatus.COMPLETED || job.getStatus() == TorrentStatus.CANCELLED) {
      return;
    }

    Path scratch = Path.of(job.getScratchPath());
    try {
      java.util.List<String> uploadedKeys = upload(job, scratch);

      for (String key : uploadedKeys) {
        keyDetailService.createOrUpdateDetail(
            job.getBucket(), key, extractName(key), contentTypeFor(key));
      }
      if (uploadedKeys.isEmpty()) {
        keyDetailService.createOrUpdateDetail(
            job.getBucket(), job.getTargetKeyPath(), extractName(job.getTargetKeyPath()), contentTypeFor(job.getTargetKeyPath()));
      }

      job.setStatus(TorrentStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());
      job.setKeyDetail(null);
      job.setProgress(1.0);
      jobService.save(job);

      registry.forget(job.getId(), job.getScratchPath());
      deleteRecursively(scratch);
      logger.info("Torrent job {} completed into {}/{}", jobId, job.getBucket(), job.getTargetKeyPath());
    } catch (Exception e) {
      logger.error("Completion failed for job {}", jobId, e);
      job.setStatus(TorrentStatus.FAILED);
      job.setErrorMessage(truncate(e.toString(), 4000));
      jobService.save(job);
    }
  }

  /**
   * Walks the scratch directory and uploads each downloaded file into the bucket.
   *
   * @return the list of uploaded S3 keys.
   */
  private java.util.List<String> upload(TorrentJobEntity job, Path scratch) throws IOException {
    boolean targetIsFolder = job.getTargetKeyPath().endsWith("/");
    java.util.HashSet<String> dirs = new java.util.HashSet<>();
    java.util.ArrayList<String> uploadedKeys = new java.util.ArrayList<>();

    Path searchDir = scratch;
    if (!Files.isDirectory(searchDir) || Files.list(searchDir).findAny().isEmpty()) {
      Path txBase = Path.of("/var/lib/transmission-daemon/downloads");
      if (Files.isDirectory(txBase)) {
        try (Stream<Path> listing = Files.list(txBase)) {
          searchDir = listing
              .filter(Files::isDirectory)
              .filter(d -> !d.getFileName().toString().startsWith("."))
              .max(java.util.Comparator.comparingLong(d -> {
                try { return Files.walk(d).filter(Files::isRegularFile).count(); } catch (Exception e) { return 0L; }
              }))
              .orElse(searchDir);
        } catch (Exception e) {
          logger.warn("Failed to scan {} for download dirs", txBase, e);
        }
      }
    }

    try (Stream<Path> paths = Files.walk(searchDir)) {
      var files = paths.filter(Files::isRegularFile).filter(this::isRealFile).toList();
      if (files.isEmpty()) {
        logger.warn("No files found to upload in {} or {}", scratch, "/var/lib/transmission-daemon/downloads/" + scratch.getFileName());
      }
      for (Path file : files) {
        String key = targetKeyFor(job, searchDir, file, targetIsFolder);
        s3Client.putObject(
            PutObjectRequest.builder().bucket(job.getBucket()).key(key).contentType(contentTypeFor(key)).build(),
            RequestBody.fromFile(file));
        uploadedKeys.add(key);
        int idx = key.lastIndexOf('/');
        while (idx > 0) {
          dirs.add(key.substring(0, idx + 1));
          idx = key.lastIndexOf('/', idx - 1);
        }
      }
      for (String dir : dirs) {
        s3Client.putObject(
            PutObjectRequest.builder().bucket(job.getBucket()).key(dir).build(),
            RequestBody.empty());
      }
    }
    if (targetIsFolder) {
      s3Client.putObject(
          PutObjectRequest.builder().bucket(job.getBucket()).key(job.getTargetKeyPath()).build(),
          RequestBody.empty());
    }
    return uploadedKeys;
  }

  /**
   * Skips libtorrent internal piece and part files.
   *
   * @return true if the file is a real download, not a libtorrent internal file.
   */
  private boolean isRealFile(Path file) {
    String name = file.getFileName().toString();
    return !name.endsWith(".parts") && !name.startsWith(".") && !name.contains(".pad");
  }

  /**
   * Maps a scratch file to its destination S3 key.
   *
   * @param scratch the base scratch directory.
   * @param file the downloaded file to map.
   */
  private String targetKeyFor(TorrentJobEntity job, Path scratch, Path file, boolean targetIsFolder) {
    String relative = scratch.relativize(file).toString().replace('\\', '/');
    if (targetIsFolder) {
      return job.getTargetKeyPath() + relative;
    }
    if (relative.contains("/")) {
      return job.getTargetKeyPath() + "/" + relative;
    }
    if (!job.getTargetKeyPath().contains(".") && relative.contains(".")) {
      return relative;
    }
    return job.getTargetKeyPath();
  }

  /**
   * Extracts the file name from a key path.
   */
  private String extractName(String keyPath) {
    String trimmed = keyPath.endsWith("/") ? keyPath.substring(0, keyPath.length() - 1) : keyPath;
    int slash = trimmed.lastIndexOf('/');
    return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
  }

  /**
   * Guesses the MIME type from a key path.
   */
  private String contentTypeFor(String keyPath) {
    String guess = java.net.URLConnection.guessContentTypeFromName(keyPath);
    return guess != null ? guess : "application/octet-stream";
  }

  /**
   * Truncates a string to a max length.
   */
  private String truncate(String value, int max) {
    return value.length() <= max ? value : value.substring(0, max);
  }

  /**
   * Deletes a directory tree recursively.
   */
  private void deleteRecursively(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(path)) {
      paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
      });
    }
  }
}

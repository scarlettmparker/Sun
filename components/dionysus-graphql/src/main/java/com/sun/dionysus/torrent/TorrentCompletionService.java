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
      upload(job, scratch);

      KeyDetailEntity detail =
          keyDetailService.createOrUpdateDetail(
              job.getBucket(), job.getTargetKeyPath(), extractName(job.getTargetKeyPath()), contentTypeFor(job.getTargetKeyPath()));
      job.setStatus(TorrentStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());
      job.setKeyDetail(detail);
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
   */
  private void upload(TorrentJobEntity job, Path scratch) throws IOException {
    boolean targetIsFolder = job.getTargetKeyPath().endsWith("/");
    try (Stream<Path> paths = Files.walk(scratch)) {
      for (Path file : paths.filter(Files::isRegularFile).filter(this::isRealFile).toList()) {
        String key = targetKeyFor(job, scratch, file, targetIsFolder);
        s3Client.putObject(
            PutObjectRequest.builder().bucket(job.getBucket()).key(key).contentType(contentTypeFor(key)).build(),
            RequestBody.fromFile(file));
      }
    }
    if (targetIsFolder) {
      s3Client.putObject(
          PutObjectRequest.builder().bucket(job.getBucket()).key(job.getTargetKeyPath()).build(),
          RequestBody.empty());
    }
  }

  /**
   * Skips libtorrent internal piece and part files.
   */
  private boolean isRealFile(Path file) {
    String name = file.getFileName().toString();
    return !name.endsWith(".parts") && !name.startsWith(".") && !name.contains(".pad");
  }

  /**
   * Maps a scratch file to its destination S3 key.
   */
  private String targetKeyFor(TorrentJobEntity job, Path scratch, Path file, boolean targetIsFolder) {
    if (!targetIsFolder) {
      return job.getTargetKeyPath();
    }
    String relative = scratch.relativize(file).toString().replace('\\', '/');
    return job.getTargetKeyPath() + relative;
  }

  private String extractName(String keyPath) {
    String trimmed = keyPath.endsWith("/") ? keyPath.substring(0, keyPath.length() - 1) : keyPath;
    int slash = trimmed.lastIndexOf('/');
    return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
  }

  private String contentTypeFor(String keyPath) {
    String guess = java.net.URLConnection.guessContentTypeFromName(keyPath);
    return guess != null ? guess : "application/octet-stream";
  }

  private String truncate(String value, int max) {
    return value.length() <= max ? value : value.substring(0, max);
  }

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

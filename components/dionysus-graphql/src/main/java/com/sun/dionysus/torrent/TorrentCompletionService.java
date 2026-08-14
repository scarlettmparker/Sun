package com.sun.dionysus.torrent;

import com.sun.dionysus.model.KeyDetailEntity;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.KeyDetailService;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

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
  @Autowired private S3Presigner s3Presigner;
  @Autowired private TorrentJobRegistry registry;
  @Autowired @Lazy private TransmissionGateway transmissionGateway;

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
      List<String> uploadedKeys = upload(job, scratch);

      if (uploadedKeys.isEmpty()) {
        logger.warn("No files uploaded for job {} - marking FAILED", jobId);
        job.setStatus(TorrentStatus.FAILED);
        job.setErrorMessage("No files found to upload after download completed");
        jobService.save(job);
        return;
      }

      // Transcode MKV/AVI files to MP4 with progress reporting
      boolean transcodeStarted = false;
      try (Stream<Path> allFiles = Files.walk(scratch)) {
        var videoFiles = allFiles.filter(Files::isRegularFile).filter(this::isVideoFile).toList();
        for (Path localFile : videoFiles) {
          String fileName = localFile.getFileName().toString();
          // Match by checking if any uploaded key ends with or starts with the filename
          String matchedKey = null;
          for (String uk : uploadedKeys) {
            String ukName = uk.contains("/") ? uk.substring(uk.lastIndexOf('/') + 1) : uk;
            if (ukName.equals(fileName) || fileName.startsWith(ukName) || ukName.startsWith(fileName)) {
              matchedKey = uk;
              break;
            }
          }

          if (matchedKey == null) continue;
          
          if (!transcodeStarted) {
            job.setStatus(TorrentStatus.TRANSCODING);
            job.setProgress(0.0);
            jobService.save(job);
            transcodeStarted = true;
          }
          try {
            Path mp4 = transcodeWithProgress(job, localFile, matchedKey);
            String mp4Key = matchedKey + ".mp4";
            putFile(job.getBucket(), mp4Key, mp4);
            uploadedKeys.remove(matchedKey);
            uploadedKeys.add(mp4Key);
            Files.deleteIfExists(mp4);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(job.getBucket()).key(matchedKey).build());
            logger.info("Transcoded and cleaned up: {}", matchedKey);
          } catch (Exception e) {
            logger.warn("Failed to transcode {} to MP4: {}", matchedKey, e.getMessage());
          }
        }
      }

      for (String key : uploadedKeys) {
        keyDetailService.createOrUpdateDetail(
            job.getBucket(), key, extractName(key), contentTypeFor(key));
      }

      job.setStatus(TorrentStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());
      job.setKeyDetail(null);
      job.setProgress(1.0);
      jobService.save(job);

      registry.forget(job.getId(), job.getScratchPath());
      transmissionGateway.removeTransmission(jobId);
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
  private List<String> upload(TorrentJobEntity job, Path scratch) throws IOException {
    boolean targetIsFolder = job.getTargetKeyPath().endsWith("/");
    HashSet<String> dirs = new HashSet<>();
    ArrayList<String> uploadedKeys = new ArrayList<>();

    Path searchDir = scratch;
    if (!Files.isDirectory(searchDir) || Files.list(searchDir).findAny().isEmpty()) {
      Path txBase = Path.of("/var/lib/transmission-daemon/downloads");
      if (Files.isDirectory(txBase)) {
        searchDir = txBase;
      }
    }

    try (Stream<Path> paths = Files.walk(searchDir)) {
      var files = paths.filter(Files::isRegularFile).filter(this::isRealFile).toList();
      if (files.isEmpty()) {
        logger.warn("No files found to upload in {}", scratch);
      }
      for (Path file : files) {
        String key = targetKeyFor(job, searchDir, file, targetIsFolder);
        putFile(job.getBucket(), key, file);
        uploadedKeys.add(key);

        int idx = key.lastIndexOf('/');
        while (idx > 0) {
          dirs.add(key.substring(0, idx + 1));
          idx = key.lastIndexOf('/', idx - 1);
        }
      }
      for (String dir : dirs) {
        putEmpty(job.getBucket(), dir);
      }
    }
    if (targetIsFolder) {
      putEmpty(job.getBucket(), job.getTargetKeyPath());
    }
    return uploadedKeys;
  }

  /**
   * Uploads a file to S3 via a presigned PUT URL, bypassing the SDK's
   * content-SHA256 signing (Garage doesn't handle it correctly).
   */
  private void putFile(String bucket, String key, Path file) throws IOException {
    String contentType = contentTypeFor(key);
    if (contentType.equals("application/octet-stream")) {
      String detected = probeContentType(file);
      if (detected != null) {
        contentType = detected;
      }
    }
    var putReq = PutObjectRequest.builder()
        .bucket(bucket).key(key).contentType(contentType).build();
    var presignReq = PutObjectPresignRequest.builder()
        .putObjectRequest(putReq)
        .signatureDuration(Duration.ofHours(1))
        .build();
    var url = s3Presigner.presignPutObject(presignReq).url();

    var conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Content-Type", contentType);
    conn.setRequestProperty("x-amz-content-sha256", "UNSIGNED-PAYLOAD");
    conn.setConnectTimeout(30000);
    conn.setReadTimeout(600000);
    conn.setFixedLengthStreamingMode(Files.size(file));
    try (var out = conn.getOutputStream(); var in = Files.newInputStream(file)) {
      in.transferTo(out);
    }
    int status = conn.getResponseCode();
    if (status < 200 || status > 299) {
      try (var err = conn.getErrorStream()) {
        String body = err == null ? "" : new String(err.readAllBytes());
        throw new IOException("Upload failed (" + status + "): " + body);
      }
    }
  }

  /**
   * Creates an empty directory marker in S3.
   */
  private void putEmpty(String bucket, String key) throws IOException {
    var putReq = PutObjectRequest.builder().bucket(bucket).key(key).build();
    var presignReq = PutObjectPresignRequest.builder()
        .putObjectRequest(putReq)
        .signatureDuration(Duration.ofHours(1))
        .build();
    var url = s3Presigner.presignPutObject(presignReq).url();

    var conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("x-amz-content-sha256", "UNSIGNED-PAYLOAD");
    conn.setConnectTimeout(10000);
    conn.setReadTimeout(10000);
    conn.setFixedLengthStreamingMode(0);
    conn.getOutputStream().close();
    int status = conn.getResponseCode();
    if (status < 200 || status > 299) {
      try (var err = conn.getErrorStream()) {
        String body = err == null ? "" : new String(err.readAllBytes());
        throw new IOException("Empty upload failed (" + status + "): " + body);
      }
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
   * True when the file is an MKV or AVI that should be transcoded to MP4.
   */
  private boolean isVideoFile(Path file) {
    String name = file.getFileName().toString().toLowerCase();
    String mime = null;
    try { mime = Files.probeContentType(file); } catch (IOException ignored) {}
    return name.endsWith(".mkv") || name.endsWith(".avi")
        || (mime != null && (mime.equals("video/x-matroska")
            || mime.equals("application/x-matroska") || mime.equals("video/avi")));
  }

  /**
   * Probes the MIME type of a file, returning null on failure.
   */
  private String probeContentType(Path file) {
    try { return Files.probeContentType(file); } catch (IOException e) { return null; }
  }

  /**
   * Maps a scratch file to its destination S3 key.
   */
  private String targetKeyFor(TorrentJobEntity job, Path scratch, Path file, boolean targetIsFolder) {
    String relative = scratch.relativize(file).toString().replace('\\', '/');
    if (targetIsFolder) {
      return job.getTargetKeyPath() + relative;
    }
    if (relative.contains("/")) {
      return job.getTargetKeyPath() + "/" + relative;
    }
    // If the local file has an extension but the target key path doesn't, use
    // the local file name to preserve the extension (e.g. .mkv for transcoding).
    if (relative.contains(".")) {
      String prefix = job.getTargetKeyPath().contains("/")
          ? job.getTargetKeyPath().substring(0, job.getTargetKeyPath().lastIndexOf('/') + 1)
          : "";
      return prefix + relative;
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
    String guess = URLConnection.guessContentTypeFromName(keyPath);
    return guess == null ? "application/octet-stream" : guess;
  }

  /**
   * Truncates a string to a max length.
   */
  private String truncate(String value, int max) {
    return value.length() <= max ? value : value.substring(0, max);
  }

  /**
   * Finds the local file in the scratch directory that matches an uploaded key.
   */
  private Path findFileByKey(Path scratch, String key) {
    String fileName = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
    try (Stream<Path> paths = Files.walk(scratch)) {
      return paths.filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().equals(fileName))
          .findFirst().orElse(null);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Transcodes an MKV/AVI file to MP4 with progress reporting. Updates the
   * job's status to TRANSCODING and reports progress via the job entity.
   */
  private Path transcodeWithProgress(TorrentJobEntity job, Path input, String key) throws IOException, InterruptedException {
    Path output = Files.createTempFile("transcode-", ".mp4");

    // Get total duration from ffprobe
    double totalDuration = 0;
    try {
      Process probe = new ProcessBuilder(
          "ffprobe", "-v", "error", "-show_entries", "format=duration",
          "-of", "default=noprint_wrappers=1:nokey=1",
          input.toAbsolutePath().toString())
          .redirectErrorStream(true).start();
      String durStr = new String(probe.getInputStream().readAllBytes()).trim();
      probe.waitFor();
      totalDuration = Double.parseDouble(durStr);
    } catch (Exception e) {
      logger.warn("Could not determine duration for {}: {}", key, e.getMessage());
    }

    var pb = new ProcessBuilder(
        "ffmpeg", "-i", input.toAbsolutePath().toString(),
        "-c:v", "libx264", "-preset", "fast",
        "-c:a", "aac",
        "-movflags", "+faststart",
        "-y", output.toAbsolutePath().toString());
    pb.redirectErrorStream(true);
    Process p = pb.start();

    var timePattern = Pattern.compile("time=(\\d+):(\\d+):(\\d+)\\.(\\d+)");
    int lineCount = 0;
    try (var reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        var m = timePattern.matcher(line);
        if (m.find() && totalDuration > 0) {
          double currentSec = Integer.parseInt(m.group(1)) * 3600
              + Integer.parseInt(m.group(2)) * 60
              + Integer.parseInt(m.group(3))
              + Integer.parseInt(m.group(4)) / 100.0;
          double prog = Math.min(currentSec / totalDuration, 1.0);
          if (++lineCount % 20 == 0) {
            job.setProgress(prog);
            jobService.save(job);
          }
        }
      }
    }

    int exit = p.waitFor();
    if (exit != 0) {
      Files.deleteIfExists(output);
      throw new IOException("ffmpeg exited with code " + exit);
    }
    return output;
  }
}

package com.sun.dionysus.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Streams S3 objects inline (for browser viewing / video playback) with
 * full Range header support for seeking. Also handles on-demand MKV
 * transcoding to MP4.
 */
@RestController
@RequestMapping("/api")
public class FileViewController {

  private static final Logger log = LoggerFactory.getLogger(FileViewController.class);

  @Autowired
  private S3Client s3Client;

  @Autowired
  private S3Presigner s3Presigner;

  /**
   * Streams the object for inline viewing. Supports HTTP Range for seeking.
   */
  @GetMapping("/view/{bucket}")
  public ResponseEntity<StreamingResponseBody> viewFile(
      @PathVariable String bucket,
      @RequestParam String key,
      HttpServletRequest request) {

    if (key == null || key.isBlank() || key.endsWith("/")) {
      return ResponseEntity.badRequest().build();
    }

    var head = headObject(bucket, key);
    if (head == null) return ResponseEntity.notFound().build();

    long contentLength = head.contentLength() != null ? head.contentLength() : -1;
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    if (head.contentType() != null) {
      try { mediaType = MediaType.parseMediaType(head.contentType()); } catch (Exception ignored) {}
    }

    String rangeHeader = request.getHeader(HttpHeaders.RANGE);
    if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
      return serveRange(bucket, key, rangeHeader, contentLength, mediaType);
    }

    var getReq = GetObjectRequest.builder().bucket(bucket).key(key).build();
    var objectStream = s3Client.getObject(getReq);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
        .contentType(mediaType)
        .contentLength(contentLength)
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .body(os -> { try (var is = objectStream) { is.transferTo(os); } });
  }

  /**
   * Handles a byte-range request (seeking in video).
   */
  private ResponseEntity<StreamingResponseBody> serveRange(
      String bucket, String key, String rangeHeader, long total, MediaType mediaType) {

    try {
      String range = rangeHeader.replace("bytes=", "").split(",")[0].trim();
      long start, end;
      if (range.startsWith("-")) {
        end = total - 1;
        start = total + Long.parseLong(range);
      } else if (range.endsWith("-")) {
        start = Long.parseLong(range.replace("-", ""));
        end = total - 1;
      } else {
        String[] parts = range.split("-");
        start = Long.parseLong(parts[0]);
        end = parts.length > 1 ? Long.parseLong(parts[1]) : total - 1;
      }
      if (start > end || start >= total) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + total)
            .build();
      }
      end = Math.min(end, total - 1);
      long length = end - start + 1;

      var getReq = GetObjectRequest.builder()
          .bucket(bucket).key(key).range("bytes=" + start + "-" + end)
          .build();
      var objectStream = s3Client.getObject(getReq);

      long finalEnd = end;
      return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
          .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
          .contentType(mediaType)
          .contentLength(length)
          .header(HttpHeaders.ACCEPT_RANGES, "bytes")
          .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + finalEnd + "/" + total)
          .body(os -> { try (var is = objectStream) { is.transferTo(os); } });
    } catch (NumberFormatException e) {
      return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
          .header(HttpHeaders.CONTENT_RANGE, "bytes */" + total)
          .build();
    }
  }

  /**
   * Transcodes an MKV/AVI file stored in S3 to MP4 and uploads it alongside
   * the original. Designed to be called after a direct upload completes or on
   * demand for existing files.
   *
   * @return 200 on success, 404 if the key is not found, 400 for non-video.
   */
  @PostMapping("/transcode/{bucket}")
  public ResponseEntity<String> transcodeFile(
      @PathVariable String bucket,
      @RequestParam String key) {

    if (key == null || key.isBlank()) {
      return ResponseEntity.badRequest().body("key is required");
    }
    if (!key.toLowerCase().endsWith(".mkv") && !key.toLowerCase().endsWith(".avi")) {
      return ResponseEntity.badRequest().body("Not an MKV or AVI file: " + key);
    }

    Path input = null;
    Path output = null;
    try {
      var head = headObject(bucket, key);
      if (head == null) return ResponseEntity.notFound().build();

      input = Files.createTempFile("transcode-input-", ".mkv");
      try (var in = s3Client.getObject(
          GetObjectRequest.builder().bucket(bucket).key(key).build())) {
        Files.copy(in, input);
      }

      output = Files.createTempFile("transcode-output-", ".mp4");
      var pb = new ProcessBuilder(
          "ffmpeg", "-i", input.toAbsolutePath().toString(),
          "-c:v", "libx264", "-preset", "fast",
          "-c:a", "aac",
          "-movflags", "+faststart",
          "-y", output.toAbsolutePath().toString());
      pb.redirectErrorStream(true);
      Process p = pb.start();
      try (var is = p.getInputStream()) {
        is.transferTo(OutputStream.nullOutputStream());
      }
      int exit = p.waitFor();
      if (exit != 0) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("ffmpeg exited with code " + exit);
      }

      String mp4Key = key + ".mp4";
      uploadViaPresign(bucket, mp4Key, output);
      log.info("Transcoded {} to {}", key, mp4Key);
      return ResponseEntity.ok(mp4Key);
    } catch (Exception e) {
      log.error("Transcoding failed for {} in bucket {}", key, bucket, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Transcoding failed: " + e.getMessage());
    } finally {
      try { if (input != null) Files.deleteIfExists(input); } catch (Exception ignored) {}
      try { if (output != null) Files.deleteIfExists(output); } catch (Exception ignored) {}
    }
  }

  /**
   * Uploads a local file to S3 via a presigned PUT URL.
   */
  private void uploadViaPresign(String bucket, String key, Path file) throws IOException {
    var presignReq = PutObjectPresignRequest.builder()
        .putObjectRequest(PutObjectRequest.builder()
            .bucket(bucket).key(key).contentType("video/mp4").build())
        .signatureDuration(Duration.ofHours(1))
        .build();
    var url = s3Presigner.presignPutObject(presignReq).url();
    var conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Content-Type", "video/mp4");
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
        String body = err != null ? new String(err.readAllBytes()) : "";
        throw new IOException("Upload failed (" + status + "): " + body);
      }
    }
  }

  /**
   * Fetches object metadata, or null on 404.
   */
  private HeadCache headObject(String bucket, String key) {
    try {
      return new HeadCache(s3Client.headObject(
          HeadObjectRequest.builder().bucket(bucket).key(key).build()));
    } catch (NoSuchKeyException e) {
      return null;
    } catch (S3Exception e) {
      if (e.statusCode() == 404) return null;
      log.error("Error checking key {} in bucket {}", key, bucket, e);
      return null;
    }
  }

  /**
   * Cached fields from a HEAD response.
   */
  private record HeadCache(Long contentLength, String contentType) {
    HeadCache(software.amazon.awssdk.services.s3.model.HeadObjectResponse r) {
      this(r.contentLength(), r.contentType());
    }
  }
}

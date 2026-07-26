package com.sun.dionysus.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Transcodes MKV/AVI files to MP4 for browser streaming.
 */
@RestController
@RequestMapping("/api")
public class TranscodeController {

  private static final Logger log = LoggerFactory.getLogger(TranscodeController.class);

  @Autowired
  private S3Client s3Client;

  /**
   * Request body for a transcode request.
   *
   * @param bucket the bucket containing the file.
   * @param key the key of the file to transcode.
   */
  private record TranscodeRequest(String bucket, String key) {}

  /**
   * Transcodes an MKV/AVI file to MP4. Downloads the original from S3,
   * runs ffmpeg, and uploads the MP4 alongside it (key + ".mp4").
   */
  @PostMapping("/transcode")
  public ResponseEntity<String> transcode(@RequestBody TranscodeRequest req) {
    String key = req.key();
    if (key == null || key.isBlank() || key.endsWith("/")) {
      return ResponseEntity.badRequest().body("Invalid key");
    }
    String lower = key.toLowerCase();
    if (!lower.endsWith(".mkv") && !lower.endsWith(".avi")) {
      return ResponseEntity.badRequest().body("Not a supported format: " + key);
    }

    Path input = null;
    Path output = null;
    try {
      input = Files.createTempFile("transcode-in-", ".mkv");
      output = Files.createTempFile("transcode-out-", ".mp4");

      s3Client.getObject(GetObjectRequest.builder().bucket(req.bucket()).key(key).build(), input);

      ProcessBuilder pb = new ProcessBuilder(
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
        throw new IOException("ffmpeg exited with code " + exit);
      }

      String mp4Key = key + ".mp4";
      s3Client.putObject(
          PutObjectRequest.builder().bucket(req.bucket()).key(mp4Key).contentType("video/mp4").build(),
          software.amazon.awssdk.core.sync.RequestBody.fromFile(output));

      log.info("Transcoded {} to {}", key, mp4Key);
      return ResponseEntity.ok(mp4Key);
    } catch (Exception e) {
      log.error("Failed to transcode {}", key, e);
      return ResponseEntity.internalServerError().body(e.getMessage());
    } finally {
      try {
        if (input != null) {
          Files.deleteIfExists(input);
        }
      } catch (IOException ignored) {
        // do nothing
      }
      try {
        if (output != null) {
          Files.deleteIfExists(output);
        }
      } catch (IOException ignored) {
        // do nothing
      }
    }
  }
}

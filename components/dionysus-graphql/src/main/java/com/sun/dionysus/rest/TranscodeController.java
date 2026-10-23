package com.sun.dionysus.rest;
import com.sun.base.audit.annotation.Audited;
import com.sun.dionysus.transcode.TranscodeResult;
import com.sun.dionysus.transcode.TranscodeService;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Transcodes MKV/AVI files to MP4 for browser streaming.
 */
@RestController
@RequestMapping("/api")
public class TranscodeController {

  private static final Logger log = LoggerFactory.getLogger(TranscodeController.class);

  @Autowired
  private S3Client s3Client;

  @Autowired
  private TranscodeService transcodeService;

  @Autowired
  @Qualifier("internalS3Presigner")
  private S3Presigner s3Presigner;

  /**
   * Request body for a transcode request.
   *
   * @param bucket the bucket containing the file.
   * @param key the key of the file to transcode.
   */
  record TranscodeRequest(String bucket, String key) {}

  /**
   * Transcodes an MKV/AVI file to MP4. Downloads the original from S3,
   * runs ffmpeg, and uploads the MP4 alongside it (key + ".mp4").
   */
  @Audited(event = "REST_POST")
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
      input = transcodeService.createWorkFile("transcode-in-", ".mkv");

      try (var in = s3Client.getObject(
          GetObjectRequest.builder().bucket(req.bucket()).key(key).build())) {
        Files.copy(in, input, StandardCopyOption.REPLACE_EXISTING);
      }

      TranscodeResult result = transcodeService.transcode(input, null);
      output = result.output();

      String mp4Key = key + ".mp4";
      uploadViaPresign(req.bucket(), mp4Key, output);

      log.info("Transcoded {} to {} using {}", key, mp4Key, result.encoder());
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
      }
      try {
        if (output != null) {
          Files.deleteIfExists(output);
        }
      } catch (IOException ignored) {
      }
    }
  }

  /**
   * Uploads a local file to S3 via a presigned PUT URL.
   *
   * @param bucket the target bucket.
   * @param key the target key.
   * @param file the local file.
   * @throws IOException when the upload fails.
   */
  void uploadViaPresign(String bucket, String key, Path file) throws IOException {
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
        String body = err == null ? "" : new String(err.readAllBytes());
        throw new IOException("Upload failed (" + status + "): " + body);
      }
    }
  }
}

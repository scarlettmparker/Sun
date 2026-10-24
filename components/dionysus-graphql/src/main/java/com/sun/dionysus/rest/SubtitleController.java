package com.sun.dionysus.rest;

import com.sun.base.audit.annotation.NotAudited;
import com.sun.dionysus.subtitle.SrtToVtt;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Serves subtitle files as WebVTT.
 */
@RestController
@RequestMapping("/api")
public class SubtitleController {

  private static final Logger log = LoggerFactory.getLogger(SubtitleController.class);

  @Autowired
  private S3Client s3Client;

  /**
   * Returns a subtitle as WebVTT.
   */
  @NotAudited(reason = "read endpoint")
  @GetMapping("/subtitles/{bucket}")
  public ResponseEntity<String> subtitle(
      @PathVariable String bucket, @RequestParam String key) {
    if (key == null || key.isBlank() || key.endsWith("/")) {
      return ResponseEntity.badRequest().build();
    }

    try {
      s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
    } catch (NoSuchKeyException e) {
      return ResponseEntity.notFound().build();
    } catch (S3Exception e) {
      if (e.statusCode() == 404) {
        return ResponseEntity.notFound().build();
      }
      log.error("Error checking subtitle {} in {}", key, bucket, e);
      return ResponseEntity.status(500).build();
    }

    try (var stream = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
      byte[] data = stream.readAllBytes();
      String vtt = SrtToVtt.convert(data, key);
      return ResponseEntity.ok()
          .contentType(MediaType.valueOf("text/vtt; charset=utf-8"))
          .body(vtt);
    } catch (IOException e) {
      log.error("Failed to read subtitle {} in {}", key, bucket, e);
      return ResponseEntity.status(500).build();
    } catch (Exception e) {
      log.error("Failed to convert subtitle {} in {}", key, bucket, e);
      return ResponseEntity.status(500).build();
    }
  }
}

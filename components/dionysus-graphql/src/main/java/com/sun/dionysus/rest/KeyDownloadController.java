package com.sun.dionysus.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * REST controller to expose S3-compatible key download operations.
 *
 * <p>
 * This controller supports downloading a single object from a bucket by
 * key. Directory-style keys are rejected with {@code 400 Bad Request}.
 */
@RestController
@RequestMapping("/api")
public class KeyDownloadController {

  private static final Logger logger = LoggerFactory.getLogger(KeyDownloadController.class);

  @Autowired
  private S3Client s3Client;

  /**
   * Downloads the object stored at the provided bucket/key location.
   *
   * @param bucket the S3 bucket name
   * @param key    the object key to download, must not end with '/'
   * @return a streaming response containing the object data or an error status
   */
  @GetMapping(path = "/buckets/{bucket}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<InputStreamResource> downloadKey(
      @PathVariable String bucket,
      @RequestParam(name = "key") String key) {

    if (key == null || key.isBlank()) {
      return ResponseEntity.badRequest().body(null);
    }

    if (key.endsWith("/")) {
      logger.warn("Requested download key is a directory: {}", key);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    HeadObjectRequest headRequest = HeadObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();

    HeadObjectResponse headResponse;
    try {
      headResponse = s3Client.headObject(headRequest);
    } catch (NoSuchKeyException notFound) {
      logger.warn("Requested key not found in bucket {}: {}", bucket, key);
      return ResponseEntity.notFound().build();
    } catch (S3Exception s3Exception) {
      if (s3Exception.statusCode() == 404) {
        return ResponseEntity.notFound().build();
      }
      logger.error("Error while checking key {} in bucket {}", key, bucket, s3Exception);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }

    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();

    ResponseInputStream<GetObjectResponse> objectStream;
    try {
      objectStream = s3Client.getObject(getObjectRequest);
    } catch (NoSuchKeyException notFound) {
      logger.warn("Requested key not found on retrieval for bucket {}: {}", bucket, key);
      return ResponseEntity.notFound().build();
    } catch (S3Exception s3Exception) {
      logger.error("Failed to download key {} from bucket {}", key, bucket, s3Exception);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }

    InputStreamResource resource = new InputStreamResource(objectStream);
    String filename = extractFilename(key);
    String contentDisposition = buildContentDisposition(filename);

    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    if (headResponse.contentType() != null) {
      try {
        mediaType = MediaType.parseMediaType(headResponse.contentType());
      } catch (Exception ignored) {}
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
        .contentType(mediaType)
        .contentLength(headResponse.contentLength() != null ? headResponse.contentLength() : -1)
        .body(resource);
  }

  /**
   * Extracts the filename from a potentially nested key path.
   *
   * @param key the full object key
   * @return the final segment of the key path
   */
  private String extractFilename(String key) {
    int lastSlash = key.lastIndexOf('/');
    if (lastSlash < 0 || lastSlash == key.length() - 1) {
      return key;
    }
    return key.substring(lastSlash + 1);
  }

  /**
   * Builds a safe Content-Disposition header value for the given filename.
   *
   * @param filename the file name to use in the response header
   * @return a Content-Disposition header string with UTF-8 filename encoding
   */
  private String buildContentDisposition(String filename) {
    String encoded;
    try {
      encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
    } catch (UnsupportedEncodingException e) {
      encoded = filename;
    }
    return "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded;
  }
}

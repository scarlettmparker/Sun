package com.sun.dionysus.graphql.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.CompletedPart;
import com.sun.dionysus.codegen.types.File;
import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.graphql.mappers.FileMapper;
import com.sun.dionysus.graphql.mappers.KeyEntryMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.time.Duration;
import software.amazon.awssdk.services.s3.S3Configuration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;

@Service
public class FilestoreGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(FilestoreGraphQLService.class);

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private S3Client s3Client;

  @Autowired
  private S3Presigner s3Presigner;

  @Autowired
  private FileMapper fileMapper;

  @Autowired
  private KeyEntryMapper keyEntryMapper;

  /**
   * Checks the health of the external filestore service.
   */
  public String health() {
    logger.info("Calling external health REST API");
    String url = "https://filestore.scarlettparker.co.uk/api/health";
    String response = restTemplate.getForObject(url, String.class);
    logger.info("Health response: {}", response);
    return response;
  }

  /**
   * Lists all available buckets via the REST API.
   */
  public List<Bucket> listBuckets() {
    logger.info("Calling ListBuckets REST API");
    String url = "https://filestore.scarlettparker.co.uk/api/v2/ListBuckets";
    ResponseEntity<Bucket[]> resp = authenticatedGet(url, Bucket[].class);
    Bucket[] buckets = resp.getBody();
    List<Bucket> result = buckets != null ? Arrays.asList(buckets) : List.of();
    logger.info("Retrieved {} buckets", result.size());
    return result;
  }

  private <T> ResponseEntity<T> authenticatedGet(String url, Class<T> responseType) {
    logger.info("Executing authenticated GET request to URL: {}", url);
    String token = System.getProperty("GARAGE_SECRET_KEY");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
    logger.info("Authenticated GET request completed with status: {}", response.getStatusCode());
    return response;
  }

  /**
   * Lists objects (files) in the given S3-compatible bucket.
   */
  public List<File> listFiles(String bucket) {
    logger.info("Listing objects in bucket: {}", bucket);
    ListObjectsV2Response resp = s3Client.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(bucket)
        .build());
    List<File> files = resp.contents().stream()
        .map(fileMapper::mapObject)
        .collect(Collectors.toList());
    logger.info("Successfully found and mapped {} files in bucket: {}", files.size(), bucket);
    return files;
  }

  /**
   * Lists keys in the given bucket using a prefix and delimiter for
   * directory-style navigation.
   */
  public List<KeyEntry> listKeys(String bucket, String prefix) {
    logger.info("Listing keys for bucket: {} with prefix: {}", bucket, prefix);
    ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
        .bucket(bucket)
        .delimiter("/");

    if (prefix != null && !prefix.isEmpty()) {
      requestBuilder.prefix(prefix);
    }

    ListObjectsV2Response resp = s3Client.listObjectsV2(requestBuilder.build());
    List<KeyEntry> entries = new ArrayList<>();

    resp.commonPrefixes().stream()
        .map(keyEntryMapper::mapDirectory)
        .forEach(entries::add);

    resp.contents().stream()
        .filter(obj -> prefix == null || !obj.key().equals(prefix))
        .map(keyEntryMapper::mapFile)
        .forEach(entries::add);

    logger.info("Found {} directory/file entries for bucket: {} with prefix: {}", entries.size(), bucket, prefix);
    return entries;
  }

  /**
   * Uploads or overwrites a file in the bucket.
   */
  public boolean putFile(String bucket, String key, String content, String contentType) {
    logger.info("Uploading file to bucket: {} with key: {}", bucket, key);
    byte[] bytes = java.util.Base64.getDecoder().decode(content);
    PutObjectRequest.Builder req = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key);
    if (contentType != null && !contentType.isEmpty()) {
      req.contentType(contentType);
    }
    s3Client.putObject(req.build(), software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));
    logger.info("Successfully uploaded file with key: {} to bucket: {}", key, bucket);
    return true;
  }

  /**
   * Creates a directory key (folder) via Garage admin REST API.
   */
  public boolean putKey(String bucket, String key) {
    String dirKey = key.endsWith("/") ? key : key + "/";
    logger.info("Creating explicit directory placeholder in bucket: {} with key: {}", bucket, dirKey);

    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(dirKey)
            .build(),
        software.amazon.awssdk.core.sync.RequestBody.empty());

    logger.info("Successfully created directory key: {} in bucket: {}", dirKey, bucket);
    return true;
  }

  /**
   * Deletes a file from the bucket.
   */
  public boolean deleteFile(String bucket, String key) {
    logger.info("Deleting object from bucket: {} with key: {}", bucket, key);
    s3Client.deleteObject(DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build());
    logger.info("Successfully deleted object: {} from bucket: {}", key, bucket);
    return true;
  }

  /**
   * Starts a multipart upload and returns the upload ID.
   */
  public String startMultipartUpload(String bucket, String key) {
    logger.info("Initiating multipart upload for bucket: {} with key: {}", bucket, key);
    CreateMultipartUploadResponse resp = s3Client.createMultipartUpload(
        CreateMultipartUploadRequest.builder()
            .bucket(bucket)
            .key(key)
            .build());
    logger.info("Multipart upload initiated. Generated Upload ID: {}", resp.uploadId());
    return resp.uploadId();
  }

  /**
   * Uploads a single part and returns its ETag.
   */
  public String uploadPart(String bucket, String key, String uploadId, int partNumber, String content) {
    logger.info("Uploading part #{} for uploadId: {} in bucket: {} with key: {}", partNumber, uploadId, bucket, key);
    byte[] bytes = java.util.Base64.getDecoder().decode(content);
    UploadPartResponse resp = s3Client.uploadPart(
        UploadPartRequest.builder()
            .bucket(bucket)
            .key(key)
            .uploadId(uploadId)
            .partNumber(partNumber)
            .build(),
        software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));
    logger.info("Part #{} upload completed. ETag: {}", partNumber, resp.eTag());
    return resp.eTag();
  }

  /**
   * Completes a multipart upload using the provided parts.
   */
  public boolean completeMultipartUpload(String bucket, String key, String uploadId, List<CompletedPart> parts) {
    logger.info("Completing multipart upload for uploadId: {} with {} total parts in bucket: {}", uploadId,
        parts.size(), bucket);
    List<software.amazon.awssdk.services.s3.model.CompletedPart> completedParts = parts.stream()
        .map(p -> software.amazon.awssdk.services.s3.model.CompletedPart.builder()
            .partNumber(p.getPartNumber())
            .eTag(p.getEtag())
            .build())
        .collect(Collectors.toList());

    s3Client.completeMultipartUpload(
        CompleteMultipartUploadRequest.builder()
            .bucket(bucket)
            .key(key)
            .uploadId(uploadId)
            .multipartUpload(CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build())
            .build());
    logger.info("Multipart upload successfully finalized for uploadId: {}", uploadId);
    return true;
  }

  /**
   * Returns a presigned PUT URL for direct browser upload (avoids base64 + GraphQL overhead).
   */
  public String getPresignedUploadUrl(String bucket, String key, String contentType) {
    logger.info("Generating presigned upload URL for {} / {}", bucket, key);

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(15))
        .putObjectRequest(b -> b
            .bucket(bucket)
            .key(key)
            .contentType(contentType != null ? contentType : "application/octet-stream"))
        .build();

    PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
    String url = presigned.url().toString();
    logger.info("Presigned URL generated (expires in 15 min)");
    return url;
  }
}
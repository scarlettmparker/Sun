package com.sun.dionysus.graphql.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;
import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.File;
import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.graphql.mappers.FileMapper;
import com.sun.dionysus.graphql.mappers.KeyEntryMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;

@Service
public class FilestoreGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(FilestoreGraphQLService.class);

  @Value("${GARAGE_SECRET_KEY}")
  private String garageSecretKey;

  @Value("${filestore.api.url:https://filestore.int.scarlettparker.co.uk}")
  private String filestoreApiUrl;

  @Autowired
  private S3Client s3Client;

  @Autowired
  private S3Presigner s3Presigner;

  @Autowired
  private FileMapper fileMapper;

  @Autowired
  private KeyEntryMapper keyEntryMapper;

  @Autowired
  private RestClient.Builder restClientBuilder;

  private RestClient restClient;

  @PostConstruct
  public void init() {
    this.restClient = restClientBuilder.build();
  }

  /**
   * Checks the health of the external filestore service.
   */
  public String health() {
    logger.info("Calling external health REST API");
    String response = restClient.get()
        .uri(filestoreApiUrl + "/api/health")
        .retrieve()
        .body(String.class);
    logger.info("Health response: {}", response);
    return response;
  }

  /**
   * Lists all available buckets via the REST API.
   */
  public List<Bucket> listBuckets() {
    logger.info("Calling ListBuckets REST API");
    Bucket[] buckets = restClient.get()
        .uri(filestoreApiUrl + "/api/v2/ListBuckets")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + garageSecretKey)
        .retrieve()
        .body(Bucket[].class);
        
    List<Bucket> result = buckets != null ? Arrays.asList(buckets) : List.of();
    logger.info("Retrieved {} buckets", result.size());
    return result;
  }

  /**
   * Lists objects (files) in the given S3-compatible bucket.
   */
  public List<File> listFiles(String bucket) {
    logger.info("Listing objects in bucket: {}", bucket);
    List<File> files = s3Client.listObjectsV2Paginator(b -> b.bucket(bucket))
        .contents().stream()
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

    List<KeyEntry> entries = new ArrayList<>();
    
    for (ListObjectsV2Response page : s3Client.listObjectsV2Paginator(requestBuilder.build())) {
      page.commonPrefixes().stream()
          .map(keyEntryMapper::mapDirectory)
          .forEach(entries::add);

      page.contents().stream()
          .filter(obj -> prefix == null || !obj.key().equals(prefix))
          .map(keyEntryMapper::mapFile)
          .forEach(entries::add);
    }

    logger.info("Found {} directory/file entries for bucket: {} with prefix: {}", entries.size(), bucket, prefix);
    return entries;
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
   * Deletes a key and all sub-keys recursively using S3 prefix listing and batch delete.
   *
   * @param bucket Target S3 bucket name.
   * @param key    Target key or folder prefix to delete.
   * @return true if successful, false if a hard exception occurred.
   */
  public boolean deleteKey(String bucket, String key) {
    if (key == null || key.isEmpty()) {
      logger.warn("Key is null or empty, aborting delete operation.");
      return false;
    }

    logger.info("Deleting key recursively from bucket: {} with key: {}", bucket, key);

    try {
      List<ObjectIdentifier> currentBatch = new ArrayList<>();
      int totalDeleted = 0;
      currentBatch.add(ObjectIdentifier.builder().key(key).build());

      // Add the prefix explicitly to catch standard nested folder structures
      String prefix = key.endsWith("/") ? key : key + "/";
      if (!prefix.equals(key)) {
        currentBatch.add(ObjectIdentifier.builder().key(prefix).build());
      }
      
      ListObjectsV2Request listReq = ListObjectsV2Request.builder()
          .bucket(bucket)
          .prefix(prefix)
          .build();

      // .contents() flattens the paginated pages into a continuous stream of S3Objects
      for (S3Object s3Object : s3Client.listObjectsV2Paginator(listReq).contents()) {
        currentBatch.add(ObjectIdentifier.builder().key(s3Object.key()).build());

        // S3 allows a max of 1000 objects per DeleteObjects request.
        // Flush the batch once we hit this limit to keep memory usage flat.
        if (currentBatch.size() >= 1000) {
          totalDeleted += flushDeleteBatch(bucket, currentBatch);
        }
      }

      // Flush any leftover objects in the final, partial batch
      if (!currentBatch.isEmpty()) {
        totalDeleted += flushDeleteBatch(bucket, currentBatch);
      }

      logger.info("Successfully deleted {} key(s) under '{}' in bucket: {}", totalDeleted, key, bucket);
      return true;

    } catch (Exception e) {
      logger.error("Failed to recursively delete key: {} from bucket: {}", key, bucket, e);
      return false;
    }
  }

  /**
   * Execute the S3 batch delete and handle potential partial failures.
   * 
   * @return The number of objects successfully deleted in this batch.
   */
  private int flushDeleteBatch(String bucket, List<ObjectIdentifier> batch) {
    DeleteObjectsRequest deleteReq = DeleteObjectsRequest.builder()
        .bucket(bucket)
        .delete(Delete.builder().objects(batch).build())
        .build();

    DeleteObjectsResponse response = s3Client.deleteObjects(deleteReq);

    // S3 Batch Delete returns HTTP 200 even if some objects fail (e.g., due to permissions).
    // We must manually check for errors in the response payload.
    if (response.hasErrors() && !response.errors().isEmpty()) {
      logger.warn("Partial failure during batch delete. {} object(s) failed to delete. First error: {}",
          response.errors().size(), response.errors().get(0).message());
    }

    int deletedCount = response.deleted().size();
    batch.clear(); 
    
    return deletedCount;
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

  /**
   * Returns a presigned GET URL for direct browser download.
   */
  public String getPresignedDownloadUrl(String bucket, String key) {
    logger.info("Generating presigned download URL for {} / {}", bucket, key);

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(15))
        .getObjectRequest(b -> b.bucket(bucket).key(key))
        .build();

    PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
    String url = presigned.url().toString();
    logger.info("Presigned download URL generated");
    return url;
  }
}
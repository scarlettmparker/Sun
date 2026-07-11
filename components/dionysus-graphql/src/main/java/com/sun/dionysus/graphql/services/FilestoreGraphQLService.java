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
import com.sun.dionysus.codegen.types.KeyDetail;
import com.sun.dionysus.codegen.types.RenameKeyResult;
import com.sun.dionysus.codegen.types.PresignInput;
import com.sun.dionysus.graphql.mappers.FileMapper;
import com.sun.dionysus.graphql.mappers.KeyEntryMapper;
import com.sun.dionysus.graphql.mappers.KeyDetailMapper;
import com.sun.dionysus.graphql.models.KeyDetailEntity;
import com.sun.dionysus.service.KeyDetailService;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
  private KeyDetailMapper keyDetailMapper;

  @Autowired
  private RestClient.Builder restClientBuilder;

  @Autowired
  private KeyDetailService keyDetailService;

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
    List<KeyDetailEntity> details = keyDetailService.listActiveForBucketAndPath(bucket, prefix);
    Map<String, KeyDetailEntity> detailMap = details.stream()
        .collect(Collectors.toMap(KeyDetailEntity::getKeyPath, d -> d, (a, b) -> a));
    
    for (ListObjectsV2Response page : s3Client.listObjectsV2Paginator(requestBuilder.build())) {
      entries.addAll(page.commonPrefixes().stream()
          .map(prefixObj -> keyEntryMapper.mapDirectory(prefixObj, detailMap.get(prefixObj.prefix())))
          .toList());

      entries.addAll(page.contents().stream()
          .filter(obj -> prefix == null || !obj.key().equals(prefix))
          .map(obj -> keyEntryMapper.mapFile(obj, detailMap.get(obj.key())))
          .toList());
    }

    logger.info("Found {} directory/file entries for bucket: {} with prefix: {}", entries.size(), bucket, prefix);
    return entries;
  }

  /**
   * Locates a single key's detailed metadata by bucket and key path.
   */
  public KeyDetail locate(String bucket, String keyPath) {
    logger.info("Locating key detail for bucket: {} at path: {}", bucket, keyPath);
    java.util.Optional<KeyDetailEntity> entity =
        keyDetailService.locateByBucketAndKeyPath(bucket, keyPath);
    return entity.map(keyDetailMapper::map).orElse(null);
  }

  /**
   * Lists all active image key details in a bucket.
   */
  public List<KeyDetail> listImages(String bucket) {
    logger.info("Listing image key details for bucket: {}", bucket);
    return keyDetailService.listImages(bucket).stream()
        .map(keyDetailMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Locates a single active image key detail by bucket and key path.
   */
  public KeyDetail locateImage(String bucket, String keyPath) {
    logger.info("Locating image key detail for bucket: {} at path: {}", bucket, keyPath);
    return keyDetailService.locateImage(bucket, keyPath).map(keyDetailMapper::map).orElse(null);
  }

  /**
   * Creates a directory key (folder) via Garage admin REST API.
   */
  public boolean putKey(String bucket, String key) {
    String dirKey;

    if (key == null || key.trim().isEmpty()) {
      // No path provided; create default at the root
      dirKey = getUniqueDefaultKey(bucket, "", "new-key");
    } else if (key.endsWith("/")) {
      // A parent path was provided without a specific child name
      dirKey = getUniqueDefaultKey(bucket, key, "new-key");
    } else {
      // An explicit exact name was provided
      dirKey = key + "/";
    }

    logger.info("Creating explicit directory placeholder in bucket: {} with key: {}", bucket, dirKey);

    s3Client.putObject(
        software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
            .bucket(bucket)
            .key(dirKey)
            .build(),
        software.amazon.awssdk.core.sync.RequestBody.empty());

    keyDetailService.createOrUpdateDetail(bucket, dirKey, dirKey, null);

    logger.info("Successfully created directory key: {} in bucket: {}", dirKey, bucket);
    return true;
  }

  /**
   * Finds the next available folder name by checking S3 for existing prefixes.
   */
  private String getUniqueDefaultKey(String bucket, String parentPath, String baseName) {
    String candidateKey = parentPath + baseName + "/";
    
    if (!folderExists(bucket, candidateKey)) {
      return candidateKey;
    }

    int counter = 1;
    while (true) {
      candidateKey = parentPath + baseName + "-" + counter + "/";
      if (!folderExists(bucket, candidateKey)) {
        return candidateKey;
      }
      counter++;
    }
  }

  /**
   * Safely checks if an S3 "folder" exists. 
   * A prefix check is necessary because a folder might exist implicitly 
   * (containing files) without a 0-byte directory marker.
   */
  private boolean folderExists(String bucket, String prefix) {
    software.amazon.awssdk.services.s3.model.ListObjectsV2Response response = 
        s3Client.listObjectsV2(
            software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .maxKeys(1)
                .build()
        );

    // If contents are returned, the prefix is currently in use
    return !response.contents().isEmpty();
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
    keyDetailService.archiveDetail(bucket, key);
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

      keyDetailService.archiveRecursive(bucket, key);

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
    return presignOne(bucket, key, contentType);
  }

  /**
   * Batch-presigns multiple PUT URLs for multi-file uploads.
   *
   * @param inputs one presign request per file
   * @return presigned URLs in the same order as the inputs
   */
  public List<String> getPresignedUploadUrls(List<PresignInput> input) {
    logger.info("Batch-presigning {} upload URLs", input.size());
    return input.stream()
        .map(input -> presignOne(input.getBucket(), input.getKey(), input.getContentType()))
        .collect(Collectors.toList());
  }

  /**
   * Generates a single presigned PUT URL (15-min expiry) and registers the key detail.
   */
  private String presignOne(String bucket, String key, String contentType) {
    logger.info("Generating presigned upload URL for {} / {}", bucket, key);

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(15))
        .putObjectRequest(b -> b
            .bucket(bucket)
            .key(key)
            .contentType(contentType != null ? contentType : "application/octet-stream"))
        .build();

    keyDetailService.createOrUpdateDetail(bucket, key, key, contentType);

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

  /**
   * Renames a key or directory prefix by copying objects to a new destination and deleting sources.
   * If merge is false, it halts and reports conflicts if any destination objects already exist.
   */
  public RenameKeyResult renameKey(String bucket, String sourceKey, String targetKey, boolean merge) {
    RenameKeyResult result = new RenameKeyResult();
    result.setSuccess(false);
    result.setHasConflicts(false);
    result.setConflicts(new ArrayList<>());

    if (sourceKey == null || targetKey == null || sourceKey.equals(targetKey)) {
      return result;
    }

    logger.info("Initiating rename in bucket '{}': '{}' -> '{}' (merge={})", bucket, sourceKey, targetKey, merge);

    try {
      // Get all objects matching the source prefix
      List<S3Object> sourceObjects = s3Client.listObjectsV2Paginator(b -> b.bucket(bucket).prefix(sourceKey))
          .contents().stream()
          .collect(Collectors.toList());

      if (sourceObjects.isEmpty()) {
        logger.warn("No objects found matching source key/prefix: {}", sourceKey);
        return result;
      }

      boolean isSourceDir = sourceKey.endsWith("/");
      String cleanSourcePrefix = isSourceDir ? sourceKey : sourceKey + "/";
      String cleanTargetPrefix = targetKey.endsWith("/") ? targetKey : targetKey + "/";

      // Map all target paths and optionally inspect for destination conflicts
      List<String> conflicts = new ArrayList<>();
      List<Map.Entry<String, String>> moves = new ArrayList<>();

      for (S3Object obj : sourceObjects) {
        String srcPath = obj.key();
        String destPath;

        if (srcPath.equals(sourceKey)) {
          // Exact match on the key itself
          destPath = isSourceDir ? cleanTargetPrefix : targetKey;
        } else if (srcPath.startsWith(cleanSourcePrefix)) {
          destPath = cleanTargetPrefix + srcPath.substring(cleanSourcePrefix.length());
        } else {
          continue;
        }

        moves.add(new AbstractMap.SimpleEntry<>(srcPath, destPath));

        if (!merge) {
          try {
            s3Client.headObject(b -> b.bucket(bucket).key(destPath));
            // Object exists at destination
            conflicts.add(destPath);
          } catch (NoSuchKeyException e) {
          }
        }
      }

      // Halt if unapproved conflicts occur
      if (!merge && !conflicts.isEmpty()) {
        logger.warn("Aborting rename operation due to {} conflicts at target destination.", conflicts.size());
        result.setHasConflicts(true);
        result.setConflicts(conflicts);
        return result;
      }

      List<ObjectIdentifier> sourceIdsToDelete = new ArrayList<>();
      for (Map.Entry<String, String> move : moves) {
        String src = move.getKey();
        String dest = move.getValue();
        
        logger.debug("Moving {} -> {}", src, dest);
        
        // Copy item to target location
        s3Client.copyObject(CopyObjectRequest.builder()
            .copySource(bucket + "/" + src)
            .destinationBucket(bucket)
            .destinationKey(dest)
            .build());

        sourceIdsToDelete.add(ObjectIdentifier.builder().key(src).build());
      }

      keyDetailService.updatePath(bucket, sourceKey, targetKey);

      // Batch clear out the old source files
      if (!sourceIdsToDelete.isEmpty()) {
        flushDeleteBatch(bucket, sourceIdsToDelete);
      }

      logger.info("Successfully completed move sequence for {} key variations.", moves.size());
      result.setSuccess(true);
      return result;

    } catch (Exception e) {
      logger.error("Failed to perform rename migration from '{}' to '{}'", sourceKey, targetKey, e);
      return result;
    }
  }
}
package com.sun.dionysus.graphql.resolvers;
import com.sun.base.audit.annotation.Audited;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.error.MutationException;
import com.sun.dionysus.codegen.types.Bucket;
import com.sun.dionysus.codegen.types.BucketKeyInput;
import com.sun.dionysus.codegen.types.DeleteFileResponse;
import com.sun.dionysus.codegen.types.DeleteKeyResponse;
import com.sun.dionysus.codegen.types.FilestoreMutations;
import com.sun.dionysus.codegen.types.FilestoreQueries;
import com.sun.dionysus.codegen.types.GetPresignedDownloadUrlResponse;
import com.sun.dionysus.codegen.types.GetPresignedUploadUrlResponse;
import com.sun.dionysus.codegen.types.GetPresignedUploadUrlsResponse;
import com.sun.dionysus.codegen.types.KeyDetail;
import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.codegen.types.PresignInput;
import com.sun.dionysus.codegen.types.PutKeyInput;
import com.sun.dionysus.codegen.types.PutKeyResponse;
import com.sun.dionysus.codegen.types.RenameKeyInput;
import com.sun.dionysus.codegen.types.RenameKeyResponse;
import com.sun.dionysus.graphql.mappers.FileMapper;
import com.sun.dionysus.graphql.mappers.KeyDetailMapper;
import com.sun.dionysus.graphql.mappers.KeyEntryMapper;
import com.sun.dionysus.model.KeyDetailEntity;
import com.sun.dionysus.model.SubtitleEntity;
import com.sun.dionysus.service.KeyDetailService;
import com.sun.dionysus.service.SubtitleService;
import com.sun.dionysus.service.torrent.TorrentJobService;
import com.sun.dionysus.torrent.TorrentJobViewService;
import jakarta.annotation.PostConstruct;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import com.sun.base.audit.annotation.NotAudited;
@DgsComponent
public class FilestoreDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(FilestoreDataFetcher.class);

  @Value("${GARAGE_SECRET_KEY}")
  private String garageSecretKey;

  @Value("${filestore.api.url:http://localhost:3903}")
  private String filestoreApiUrl;

  @Value("${filestore.presign.download-ttl:24h}")
  private Duration downloadPresignTtl = Duration.ofHours(24);

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final FileMapper fileMapper;
  private final KeyEntryMapper keyEntryMapper;
  private final KeyDetailMapper keyDetailMapper;
  private final RestClient.Builder restClientBuilder;
  private final KeyDetailService keyDetailService;
  private final SubtitleService subtitleService;
  private final TorrentJobService torrentJobService;
  private final TorrentJobViewService torrentJobViewService;

  private RestClient restClient;

  public FilestoreDataFetcher(S3Client s3Client, @Qualifier("s3Presigner") S3Presigner s3Presigner,
      FileMapper fileMapper, KeyEntryMapper keyEntryMapper, KeyDetailMapper keyDetailMapper,
      RestClient.Builder restClientBuilder, KeyDetailService keyDetailService,
      SubtitleService subtitleService,
      TorrentJobService torrentJobService, TorrentJobViewService torrentJobViewService) {
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.fileMapper = fileMapper;
    this.keyEntryMapper = keyEntryMapper;
    this.keyDetailMapper = keyDetailMapper;
    this.restClientBuilder = restClientBuilder;
    this.keyDetailService = keyDetailService;
    this.subtitleService = subtitleService;
    this.torrentJobService = torrentJobService;
    this.torrentJobViewService = torrentJobViewService;
  }

  @PostConstruct
  public void init() {
    this.restClient = restClientBuilder.build();
  }

  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Query", field = "filestoreQueries")
  public FilestoreQueries getFilestoreQueries() {
    return FilestoreQueries.newBuilder().build();
  }

  /**
   * Checks the health of the external filestore service.
   */
  @Audited(event = "HEALTH")
  @DgsData(parentType = "FilestoreQueries", field = "health")
  public String health() {
    logger.info("Calling external health REST API");
    String response = restClient.get()
        .uri(filestoreApiUrl + "/health")
        .retrieve()
        .body(String.class);
    logger.info("Health response: {}", response);
    return response;
  }

  /**
   * Lists all available buckets via the REST API.
   */
  @Audited(event = "LISTBUCKETS", entity = "Bucket")
  @DgsData(parentType = "FilestoreQueries", field = "listBuckets")
  @PreAuthorize("@permissions.has('graphql.dionysus.listBuckets')")
  public List<Bucket> listBuckets() {
    logger.info("Calling ListBuckets REST API");
    Bucket[] buckets = restClient.get()
        .uri(filestoreApiUrl + "/v2/ListBuckets")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + garageSecretKey)
        .retrieve()
        .body(Bucket[].class);

    List<Bucket> result = buckets == null ? List.of() : Arrays.asList(buckets);
    logger.info("Retrieved {} buckets", result.size());
    return result;
  }

  /**
   * Lists keys in the given bucket using a prefix and delimiter for
   * directory-style navigation.
   */
  @Audited(event = "LISTKEYS", entity = "Key")
  @DgsData(parentType = "FilestoreQueries", field = "listKeys")
  @PreAuthorize("@permissions.has('graphql.dionysus.listKeys')")
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

    mergeInProgressTorrentJobs(bucket, prefix, entries);

    logger.info("Found {} directory/file entries for bucket: {} with prefix: {}", entries.size(), bucket, prefix);
    return entries;
  }

  /**
   * Locates a single key's detailed metadata by bucket and key path.
   */
  @Audited(event = "LOCATE")
  @DgsData(parentType = "FilestoreQueries", field = "locate")
  @PreAuthorize("@permissions.has('graphql.dionysus.locate')")
  public KeyDetail locate(String bucket, String keyPath) {
    logger.info("Locating key detail for bucket: {} at path: {}", bucket, keyPath);
    Optional<KeyDetailEntity> entity =
        keyDetailService.locateByBucketAndKeyPath(bucket, keyPath);
    return entity.map(keyDetailMapper::map).orElse(null);
  }

  /**
   * Lists all active image key details in a bucket.
   */
  @Audited(event = "LISTIMAGES", entity = "Image")
  @DgsData(parentType = "FilestoreQueries", field = "listImages")
  @PreAuthorize("@permissions.has('graphql.dionysus.listImages')")
  public List<KeyDetail> listImages(String bucket) {
    logger.info("Listing image key details for bucket: {}", bucket);
    return keyDetailService.listImages(bucket).stream()
        .map(keyDetailMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Locates a single active image key detail by bucket and key path.
   */
  @Audited(event = "LOCATEIMAGE", entity = "Image")
  @DgsData(parentType = "FilestoreQueries", field = "locateImage")
  @PreAuthorize("@permissions.has('graphql.dionysus.locateImage')")
  public KeyDetail locateImage(String bucket, String keyPath) {
    logger.info("Locating image key detail for bucket: {} at path: {}", bucket, keyPath);
    return keyDetailService.locateImage(bucket, keyPath).map(keyDetailMapper::map).orElse(null);
  }

  @NotAudited(reason = "namespace root")
  @DgsData(parentType = "Mutation", field = "filestoreMutations")
  public FilestoreMutations getFilestoreMutations() {
    return FilestoreMutations.newBuilder().build();
  }

  /**
   * Creates a directory key (folder) via Garage admin REST API.
   */
  @Audited(event = "PUTKEY", entity = "Key")
  @DgsData(parentType = "FilestoreMutations", field = "putKey")
  @PreAuthorize("@permissions.has('graphql.dionysus.putKey')")
  public PutKeyResponse putKey(PutKeyInput input) {
    String bucket = input.getBucket();
    String key = input.getKey();
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

    var putReq = software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
        .bucket(bucket)
        .key(dirKey)
        .build();
    var presignReq = PutObjectPresignRequest.builder()
        .putObjectRequest(putReq)
        .signatureDuration(Duration.ofHours(1))
        .build();
    var url = s3Presigner.presignPutObject(presignReq).url();

    try {
      var conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("PUT");
      conn.setRequestProperty("x-amz-content-sha256", "UNSIGNED-PAYLOAD");
      conn.setFixedLengthStreamingMode(0);
      conn.getOutputStream().close();
      int status = conn.getResponseCode();
      if (status < 200 || status > 299) {
        throw new MutationException("putKey failed: storage returned status " + status);
      }
    } catch (MutationException e) {
      throw e;
    } catch (Exception e) {
      throw new MutationException("putKey failed: " + e.getMessage(), e);
    }

    keyDetailService.createOrUpdateDetail(bucket, dirKey, dirKey, null);

    logger.info("Successfully created directory key: {} in bucket: {}", dirKey, bucket);
    return PutKeyResponse.newBuilder()
        .message("putKey succeeded")
        .key(dirKey)
        .build();
  }

  /**
   * Deletes a file from the bucket.
   */
  @Audited(event = "DELETEFILE", entity = "File")
  @DgsData(parentType = "FilestoreMutations", field = "deleteFile")
  @PreAuthorize("@permissions.has('graphql.dionysus.deleteFile')")
  public DeleteFileResponse deleteFile(BucketKeyInput input) {
    String bucket = input.getBucket();
    String key = input.getKey();
    if (torrentJobService.hasActiveAt(bucket, key)) {
      logger.warn("Rejecting delete of {} / {}: active torrent job exists", bucket, key);
      throw new MutationException("deleteFile failed: an active torrent job exists for " + key);
    }
    logger.info("Deleting object from bucket: {} with key: {}", bucket, key);
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build());
      keyDetailService.archiveDetail(bucket, key);
      subtitleService.deleteForVideo(bucket, key);
      deleteSubtitleSiblings(bucket, key);
    } catch (Exception e) {
      throw new MutationException("deleteFile failed: " + e.getMessage(), e);
    }
    logger.info("Successfully deleted object: {} from bucket: {}", key, bucket);
    return DeleteFileResponse.newBuilder()
        .message("deleteFile succeeded")
        .key(key)
        .build();
  }

  /**
   * Deletes a key and all sub-keys recursively using S3 prefix listing and batch delete.
   *
   * @param bucket Target S3 bucket name.
   * @param key    Target key or folder prefix to delete.
   * @return the deleted key
   */
  @Audited(event = "DELETEKEY", entity = "Key")
  @DgsData(parentType = "FilestoreMutations", field = "deleteKey")
  @PreAuthorize("@permissions.has('graphql.dionysus.deleteKey')")
  public DeleteKeyResponse deleteKey(BucketKeyInput input) {
    String bucket = input.getBucket();
    String key = input.getKey();
    if (key == null || key.isEmpty()) {
      throw new MutationException("deleteKey failed: key must not be empty");
    }

    if (hasActiveAtOrChild(bucket, key)) {
      logger.warn("Rejecting recursive delete of {} / {}: active torrent job exists", bucket, key);
      throw new MutationException("deleteKey failed: an active torrent job exists for " + key);
    }

    logger.info("Deleting key recursively from bucket: {} with key: {}", bucket, key);

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
    subtitleService.deleteUnderPrefix(bucket, key);

    logger.info("Successfully deleted {} key(s) under '{}' in bucket: {}", totalDeleted, key, bucket);
    return DeleteKeyResponse.newBuilder()
        .message("deleteKey succeeded")
        .key(key)
        .build();
  }

  /**
   * Renames a key or directory prefix by copying objects to a new destination and deleting sources.
   * If merge is false, it halts and reports conflicts if any destination objects already exist.
   */
  @Audited(event = "RENAMEKEY", entity = "Key")
  @DgsData(parentType = "FilestoreMutations", field = "renameKey")
  @PreAuthorize("@permissions.has('graphql.dionysus.renameKey')")
  public RenameKeyResponse renameKey(RenameKeyInput input) {
    String bucket = input.getBucket();
    String sourceKey = input.getSourceKey();
    String targetKey = input.getTargetKey();
    boolean merge = input.getMerge();
    if (sourceKey == null || targetKey == null || sourceKey.equals(targetKey)) {
      return RenameKeyResponse.newBuilder()
          .message("renameKey failed: source and target must be provided and differ")
          .success(false)
          .hasConflicts(false)
          .conflicts(new ArrayList<>())
          .build();
    }

    if (hasActiveAtOrChild(bucket, sourceKey)) {
      logger.warn("Rejecting rename of {} / {}: active torrent job exists", bucket, sourceKey);
      return RenameKeyResponse.newBuilder()
          .message("renameKey failed: an active torrent job exists for " + sourceKey)
          .success(false)
          .hasConflicts(false)
          .conflicts(new ArrayList<>())
          .build();
    }

    logger.info("Initiating rename in bucket '{}': '{}' -> '{}' (merge={})", bucket, sourceKey, targetKey, merge);

    // Get all objects matching the source prefix
    List<S3Object> sourceObjects = s3Client.listObjectsV2Paginator(b -> b.bucket(bucket).prefix(sourceKey))
        .contents().stream()
        .collect(Collectors.toList());

    if (sourceObjects.isEmpty()) {
      logger.warn("No objects found matching source key/prefix: {}", sourceKey);
      return RenameKeyResponse.newBuilder()
          .message("renameKey failed: no objects found at " + sourceKey)
          .success(false)
          .hasConflicts(false)
          .conflicts(new ArrayList<>())
          .build();
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
      return RenameKeyResponse.newBuilder()
          .message("renameKey failed: " + conflicts.size() + " conflict(s) at target destination")
          .success(false)
          .hasConflicts(true)
          .conflicts(conflicts)
          .build();
    }

    List<ObjectIdentifier> sourceIdsToDelete = new ArrayList<>();

    // Move sibling subtitle files for a single renamed video.
    if (!isSourceDir) {
      String parent = parentPrefix(sourceKey);
      String listPrefix = parent + stemOf(sourceKey);
      ListObjectsV2Request siblingReq = ListObjectsV2Request.builder()
          .bucket(bucket)
          .prefix(listPrefix)
          .build();
      var siblingPaginator = s3Client.listObjectsV2Paginator(siblingReq);
      if (siblingPaginator != null) {
        for (S3Object sibling : siblingPaginator.contents()) {
        String siblingName = basename(sibling.key());
        String ext = extension(siblingName);
        if (!isSubtitleExtension(ext)) {
          continue;
        }
        String base = siblingName.substring(0, siblingName.length() - ext.length() - 1);
        String sourceStem = stemOf(sourceKey);
        if (!base.equals(sourceStem) && !base.startsWith(sourceStem + ".")) {
          continue;
        }
        if (moves.stream().anyMatch(e -> e.getKey().equals(sibling.key()))) {
          continue;
        }
        String targetName = stemOf(targetKey) + base.substring(sourceStem.length()) + "." + ext;
        String dest = parent + targetName;
        s3Client.copyObject(CopyObjectRequest.builder()
            .copySource(bucket + "/" + sibling.key())
            .destinationBucket(bucket)
            .destinationKey(dest)
            .build());
        sourceIdsToDelete.add(ObjectIdentifier.builder().key(sibling.key()).build());
      }
      }

      for (SubtitleEntity subtitle : subtitleService.listForVideo(bucket, sourceKey)) {
        String oldKey = subtitle.getSubtitleKey();
        String oldName = basename(oldKey);
        String ext = extension(oldName);
        String oldBase = ext.isBlank()
            ? oldName
            : oldName.substring(0, oldName.length() - ext.length() - 1);
        String sourceStem = stemOf(sourceKey);
        String newBase = stemOf(targetKey) + (oldBase.startsWith(sourceStem)
            ? oldBase.substring(sourceStem.length())
            : "");
        String newKey = parentPrefix(oldKey) + newBase + (ext.isBlank() ? "" : "." + ext);
        if (sourceIdsToDelete.stream().noneMatch(id -> id.key().equals(oldKey))) {
          try {
            s3Client.copyObject(CopyObjectRequest.builder()
                .copySource(bucket + "/" + oldKey)
                .destinationBucket(bucket)
                .destinationKey(newKey)
                .build());
            sourceIdsToDelete.add(ObjectIdentifier.builder().key(oldKey).build());
          } catch (Exception e) {
            logger.warn("Failed to move subtitle {}: {}", oldKey, e.getMessage());
          }
        }
        subtitleService.deleteBySubtitleKey(bucket, oldKey);
        subtitleService.attach(bucket, targetKey, newKey, subtitle.getLanguage(),
            subtitle.getSource(), subtitle.getOpenSubtitlesFileId());
      }
    }

    // Batch clear out the old source files
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
    subtitleService.updatePath(bucket, sourceKey, targetKey);

    // Batch clear out the old source files
    if (!sourceIdsToDelete.isEmpty()) {
      flushDeleteBatch(bucket, sourceIdsToDelete);
    }

    logger.info("Successfully completed move sequence for {} key variations.", moves.size());
    return RenameKeyResponse.newBuilder()
        .message("renameKey succeeded")
        .success(true)
        .hasConflicts(false)
        .conflicts(new ArrayList<>())
        .build();
  }

  /**
   * Returns a presigned PUT URL for direct browser upload (avoids base64 + GraphQL overhead).
   */
  @Audited(event = "GETPRESIGNEDUPLOADURL", entity = "PresignedUploadUrl")
  @DgsData(parentType = "FilestoreMutations", field = "getPresignedUploadUrl")
  @PreAuthorize("@permissions.has('graphql.dionysus.getPresignedUploadUrl')")
  public GetPresignedUploadUrlResponse getPresignedUploadUrl(PresignInput input) {
    String url = presignOne(input.getBucket(), input.getKey(), input.getContentType());
    return GetPresignedUploadUrlResponse.newBuilder()
        .message("getPresignedUploadUrl succeeded")
        .url(url)
        .build();
  }

  /**
   * Batch-presigns multiple PUT URLs for multi-file uploads.
   *
   * @param input one presign request per file
   * @return the presigned URLs in the same order as the inputs
   */
  @Audited(event = "GETPRESIGNEDUPLOADURLS", entity = "PresignedUploadUrl")
  @DgsData(parentType = "FilestoreMutations", field = "getPresignedUploadUrls")
  @PreAuthorize("@permissions.has('graphql.dionysus.getPresignedUploadUrls')")
  public GetPresignedUploadUrlsResponse getPresignedUploadUrls(
      List<PresignInput> input) {
    logger.info("Batch-presigning {} upload URLs", input.size());
    List<String> urls = input.stream()
        .map(item -> presignOne(item.getBucket(), item.getKey(), item.getContentType()))
        .collect(Collectors.toList());
    return GetPresignedUploadUrlsResponse.newBuilder()
        .message("getPresignedUploadUrls succeeded")
        .urls(urls)
        .build();
  }

  /**
   * Returns a presigned GET URL for direct browser download.
   */
  @Audited(event = "GETPRESIGNEDDOWNLOADURL", entity = "PresignedDownloadUrl")
  @DgsData(parentType = "FilestoreMutations", field = "getPresignedDownloadUrl")
  @PreAuthorize("@permissions.has('graphql.dionysus.getPresignedDownloadUrl')")
  public GetPresignedDownloadUrlResponse getPresignedDownloadUrl(BucketKeyInput input) {
    String bucket = input.getBucket();
    String key = input.getKey();
    logger.info("Generating presigned download URL for {} / {}", bucket, key);

    String url = presignDownload(bucket, key);
    logger.info("Presigned download URL generated (valid for {})", downloadPresignTtl);
    return GetPresignedDownloadUrlResponse.newBuilder()
        .message("getPresignedDownloadUrl succeeded")
        .url(url)
        .build();
  }

  /**
   * Folds in-progress torrent jobs into the listing so a downloading torrent
   * appears as a key even though no S3 object exists yet.
   */
  private void mergeInProgressTorrentJobs(String bucket, String prefix, List<KeyEntry> entries) {
    Map<String, KeyEntry> byKey =
        entries.stream().collect(Collectors.toMap(KeyEntry::getKey, e -> e, (a, b) -> a));

    entries.addAll(torrentJobViewService.mergeInProgressJobs(bucket, prefix, byKey));
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
   * Generates a presigned GET URL using the configured download TTL.
   *
   * @param bucket the bucket holding the object
   * @param key the object key
   * @return the presigned URL
   */
  private String presignDownload(String bucket, String key) {
    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(downloadPresignTtl)
        .getObjectRequest(b -> b.bucket(bucket).key(key))
        .build();

    PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
    return presigned.url().toString();
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
            .contentType(contentType == null ? "application/octet-stream" : contentType))
        .build();

    keyDetailService.createOrUpdateDetail(bucket, key, key, contentType);

    PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
    String url = presigned.url().toString();
    logger.info("Presigned URL generated (expires in 15 min)");
    return url;
  }

  /**
   * True when an active torrent job targets the given key or a child path
   * under it (for folder-level operations).
   */
  private boolean hasActiveAtOrChild(String bucket, String key) {
    return torrentJobService.hasActiveAtOrUnder(bucket, key);
  }

  private void deleteSubtitleSiblings(String bucket, String key) {
    String parent = parentPrefix(key);
    String stem = stemOf(key);
    ListObjectsV2Request request = ListObjectsV2Request.builder()
        .bucket(bucket)
        .prefix(parent + stem)
        .build();
    List<ObjectIdentifier> ids = new ArrayList<>();
    var deletePaginator = s3Client.listObjectsV2Paginator(request);
    if (deletePaginator != null) {
      for (S3Object object : deletePaginator.contents()) {
      String name = basename(object.key());
      String ext = extension(name);
      if (!isSubtitleExtension(ext)) {
        continue;
      }
      String base = name.substring(0, name.length() - ext.length() - 1);
      if (!base.equals(stem) && !base.startsWith(stem + ".")) {
        continue;
      }
      ids.add(ObjectIdentifier.builder().key(object.key()).build());
    }
    }
    if (!ids.isEmpty()) {
      flushDeleteBatch(bucket, ids);
    }
  }

  private static String parentPrefix(String key) {
    int slash = key.lastIndexOf('/');
    return slash >= 0 ? key.substring(0, slash + 1) : "";
  }

  private static String stemOf(String key) {
    String name = basename(key);
    int dot = name.lastIndexOf('.');
    return dot > 0 ? name.substring(0, dot) : name;
  }

  private static String basename(String key) {
    int slash = key.lastIndexOf('/');
    return slash >= 0 ? key.substring(slash + 1) : key;
  }

  private static String extension(String name) {
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
      return "";
    }
    return name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
  }

  private static boolean isSubtitleExtension(String ext) {
    return ext.equals("srt") || ext.equals("vtt") || ext.equals("ass") || ext.equals("ssa");
  }

}

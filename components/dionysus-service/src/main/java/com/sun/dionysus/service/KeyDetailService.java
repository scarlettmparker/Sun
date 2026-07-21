package com.sun.dionysus.service;

import com.sun.dionysus.model.KeyDetailEntity;
import com.sun.dionysus.model.enums.Status;
import com.sun.dionysus.repository.KeyDetailEntityRepository;
import com.sun.base.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing KeyDetail entities - metadata for S3 keys (files/folders).
 * Handles creation, archival, and path updates.
 */
@Service
@Transactional
public class KeyDetailService extends BaseService<KeyDetailEntity> {

  private static final Logger logger = LoggerFactory.getLogger(KeyDetailService.class);

  private final KeyDetailEntityRepository keyDetailRepository;

  public KeyDetailService(KeyDetailEntityRepository repository) {
    super(repository);
    this.keyDetailRepository = repository;
  }

  /**
   * Lists all active KeyDetail records matching a bucket and key path prefix.
   *
   * @param bucket the S3 bucket name
   * @param prefix the key path prefix (can be null for root)
   * @return list of active KeyDetail entities
   */
  public List<KeyDetailEntity> listActiveForBucketAndPath(String bucket, String prefix) {
    logger.debug("Listing active key details for bucket: {} with prefix: {}", bucket, prefix);
    if (prefix == null) {
      return keyDetailRepository.findByBucketAndKeyPathStartingWithAndStatus(bucket, "", Status.ACTIVE);
    }
    return keyDetailRepository.findByBucketAndKeyPathStartingWithAndStatus(bucket, prefix, Status.ACTIVE);
  }

  /**
   * Extracts the display name from a key path - the last segment after the final slash.
   *
   * @param keyPath the full S3 key path
   * @return the display name (e.g. "photo.jpg" from "photos/vacation/photo.jpg")
   */
  private String extractName(String keyPath) {
    int lastSlash = keyPath.lastIndexOf("/");
    return lastSlash >= 0 ? keyPath.substring(lastSlash + 1) : keyPath;
  }

  /**
   * Creates a new or updates an existing KeyDetail record.
   * Sets status to ACTIVE and description to empty if not provided.
   *
   * @param bucket the S3 bucket name
   * @param keyPath the S3 key path
   * @param name the display name
   * @param contentType the MIME content type, or null to leave unchanged
   * @return the saved KeyDetail entity
   */
  public KeyDetailEntity createOrUpdateDetail(String bucket, String keyPath, String name, String contentType) {
    logger.debug("Creating or updating key detail for bucket: {} at path: {}", bucket, keyPath);

    List<KeyDetailEntity> existing = keyDetailRepository.findByBucketAndKeyPath(bucket, keyPath);
    KeyDetailEntity detail;

    if (existing != null && !existing.isEmpty()) {
      detail = existing.get(0);
      detail.setBucket(bucket);
      detail.setKeyPath(keyPath);
      detail.setName(extractName(keyPath));
      detail.setStatus(Status.ACTIVE);
      detail.setArchivedAt(null);
      if (contentType != null) {
        detail.setContentType(contentType);
      }
    } else {
      detail = new KeyDetailEntity();
      detail.setBucket(bucket);
      detail.setKeyPath(keyPath);
      detail.setName(extractName(keyPath));
      detail.setDescription("");
      detail.setStatus(Status.ACTIVE);
      detail.setContentType(contentType);
    }

    return keyDetailRepository.save(detail);
  }

  /**
   * Lists all active image key details in a bucket.
   *
   * @param bucket the S3 bucket name
   * @return the active image key details
   */
  public List<KeyDetailEntity> listImages(String bucket) {
    logger.debug("Listing image key details for bucket: {}", bucket);
    return keyDetailRepository.findByBucketAndContentTypeStartingWithAndStatus(bucket, "image/", Status.ACTIVE);
  }

  /**
   * Locates a single active image key detail by bucket and key path.
   *
   * @param bucket the S3 bucket name
   * @param keyPath the S3 key path
   * @return Optional containing the image key detail if found and active
   */
  public Optional<KeyDetailEntity> locateImage(String bucket, String keyPath) {
    logger.debug("Locating image key detail for bucket: {} at path: {}", bucket, keyPath);
    return keyDetailRepository.findByBucketAndKeyPathAndContentTypeStartingWithAndStatus(bucket, keyPath, "image/", Status.ACTIVE);
  }

  /**
   * Locates a single active KeyDetail by bucket and key path.
   *
   * @param bucket the S3 bucket name
   * @param keyPath the S3 key path
   * @return Optional containing the KeyDetail if found and active
   */
  public Optional<KeyDetailEntity> locateByBucketAndKeyPath(String bucket, String keyPath) {
    logger.debug("Locating key detail for bucket: {} at path: {}", bucket, keyPath);
    return keyDetailRepository.findByBucketAndKeyPathAndStatus(bucket, keyPath, Status.ACTIVE);
  }

  /**
   * Archives a single KeyDetail by bucket and key path.
   * Sets status to ARCHIVED and records the archival timestamp.
   *
   * @param bucket the S3 bucket name
   * @param keyPath the S3 key path
   */
  public void archiveDetail(String bucket, String keyPath) {
    logger.info("Archiving key detail for bucket: {} at path: {}", bucket, keyPath);

    List<KeyDetailEntity> list = keyDetailRepository.findByBucketAndKeyPath(bucket, keyPath);
    for (KeyDetailEntity d : list) {
      d.setStatus(Status.ARCHIVED);
      d.setArchivedAt(LocalDateTime.now());
      keyDetailRepository.save(d);
    }
  }

  /**
   * Archives all KeyDetail records under a prefix recursively.
   *
   * @param bucket the S3 bucket name
   * @param keyPrefix the S3 key prefix to archive
   */
  public void archiveRecursive(String bucket, String keyPrefix) {
    logger.info("Archiving key details recursively for bucket: {} with prefix: {}", bucket, keyPrefix);

    String prefix = keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
    List<KeyDetailEntity> list = keyDetailRepository.findByBucketAndKeyPathStartingWith(bucket, prefix);

    for (KeyDetailEntity d : list) {
      d.setStatus(Status.ARCHIVED);
      d.setArchivedAt(LocalDateTime.now());
      keyDetailRepository.save(d);
    }
  }

  /**
   * Updates the key path for all KeyDetail records matching a source prefix.
   * Used when a key/folder is renamed or moved in S3.
   *
   * @param bucket the S3 bucket name
   * @param sourceKey the original key path
   * @param targetKey the new key path
   */
  public void updatePath(String bucket, String sourceKey, String targetKey) {
    logger.info("Updating key paths in bucket: {} from {} to {}", bucket, sourceKey, targetKey);

    String srcPrefix = sourceKey.endsWith("/") ? sourceKey : sourceKey + "/";
    String tgtPrefix = targetKey.endsWith("/") ? targetKey : targetKey + "/";

    List<KeyDetailEntity> list = keyDetailRepository.findByBucketAndKeyPathStartingWith(bucket, srcPrefix);

    if (!sourceKey.endsWith("/")) {
      List<KeyDetailEntity> exact = keyDetailRepository.findByBucketAndKeyPath(bucket, sourceKey);
      for (KeyDetailEntity entity : exact) {
        if (list.stream().noneMatch(e -> e.getKeyPath().equals(entity.getKeyPath()))) {
          list.add(entity);
        }
      }
    }

    for (KeyDetailEntity d : list) {
      String kp = d.getKeyPath();
      String newKp;
      if (kp.equals(sourceKey)) {
        newKp = targetKey;
      } else if (kp.startsWith(srcPrefix)) {
        String suffix = kp.substring(srcPrefix.length());
        newKp = tgtPrefix + suffix;
      } else {
        continue;
      }
      d.setKeyPath(newKp);
      d.setName(extractName(newKp));
      keyDetailRepository.save(d);
    }
  }
}

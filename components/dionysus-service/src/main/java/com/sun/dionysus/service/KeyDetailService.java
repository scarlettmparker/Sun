package com.sun.dionysus.service;

import com.sun.dionysus.graphql.models.KeyDetail;
import com.sun.dionysus.graphql.models.Status;
import com.sun.dionysus.service.repository.KeyDetailRepository;
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
@Transactional("dionysusTransactionManager")
public class KeyDetailService extends BaseService<KeyDetail> {

  private static final Logger logger = LoggerFactory.getLogger(KeyDetailService.class);

  private final KeyDetailRepository keyDetailRepository;

  public KeyDetailService(KeyDetailRepository repository) {
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
  public List<KeyDetail> listActiveForBucketAndPath(String bucket, String prefix) {
    logger.debug("Listing active key details for bucket: {} with prefix: {}", bucket, prefix);
    if (prefix == null) {
      return keyDetailRepository.findByBucketAndKeyPathStartingWithAndStatus(bucket, "", Status.ACTIVE);
    }
    return keyDetailRepository.findByBucketAndKeyPathStartingWithAndStatus(bucket, prefix, Status.ACTIVE);
  }

  /**
   * Creates a new or updates an existing KeyDetail record.
   * Sets status to ACTIVE and description to empty if not provided.
   *
   * @param bucket the S3 bucket name
   * @param keyPath the S3 key path
   * @param name the display name (typically the file/folder name)
   * @return the saved KeyDetail entity
   */
  public KeyDetail createOrUpdateDetail(String bucket, String keyPath, String name) {
    logger.debug("Creating or updating key detail for bucket: {} at path: {}", bucket, keyPath);

    List<KeyDetail> existing = keyDetailRepository.findByBucketAndKeyPath(bucket, keyPath);
    KeyDetail detail;

    if (existing != null && !existing.isEmpty()) {
      detail = existing.get(0);
      detail.setBucket(bucket);
      detail.setKeyPath(keyPath);
      detail.setName(name != null ? name : detail.getName());
      detail.setStatus(Status.ACTIVE);
      detail.setArchivedAt(null);
    } else {
      detail = new KeyDetail();
      detail.setBucket(bucket);
      detail.setKeyPath(keyPath);
      detail.setName(name);
      detail.setDescription("");
      detail.setStatus(Status.ACTIVE);
    }

    return keyDetailRepository.save(detail);
  }

  /**
   * Locates a single active KeyDetail by bucket and key path.
   *
   * @param bucket the S3 bucket name
   * @param keyPath the S3 key path
   * @return Optional containing the KeyDetail if found and active
   */
  public Optional<KeyDetail> locateByBucketAndKeyPath(String bucket, String keyPath) {
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

    List<KeyDetail> list = keyDetailRepository.findByBucketAndKeyPath(bucket, keyPath);
    for (KeyDetail d : list) {
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
    List<KeyDetail> list = keyDetailRepository.findByBucketAndKeyPathStartingWith(bucket, prefix);

    for (KeyDetail d : list) {
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

    List<KeyDetail> list = keyDetailRepository.findByBucketAndKeyPathStartingWith(bucket, srcPrefix);

    for (KeyDetail d : list) {
      String kp = d.getKeyPath();
      if (kp.equals(sourceKey)) {
        d.setKeyPath(targetKey);
      } else if (kp.startsWith(srcPrefix)) {
        String suffix = kp.substring(srcPrefix.length());
        d.setKeyPath(tgtPrefix + suffix);
      }
      keyDetailRepository.save(d);
    }
  }
}

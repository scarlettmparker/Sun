package com.sun.dionysus.service;

import com.sun.base.service.BaseService;
import com.sun.dionysus.model.SubtitleEntity;
import com.sun.dionysus.model.enums.SubtitleSource;
import com.sun.dionysus.repository.SubtitleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages subtitle objects attached to video keys.
 */
@Service
@Transactional
public class SubtitleService extends BaseService<SubtitleEntity> {

  private static final Logger logger = LoggerFactory.getLogger(SubtitleService.class);
  private static final String DEFAULT_LANGUAGE = "en";
  private static final String REMOTE_TYPE = "subtitle";

  private final SubtitleRepository subtitleRepository;

  public SubtitleService(SubtitleRepository repository) {
    super(repository);
    this.subtitleRepository = repository;
  }

  /**
   * Creates or updates the subtitle object for a video key.
   *
   * @param bucket the bucket holding both objects
   * @param keyPath the video key
   * @param subtitleKey the subtitle object key
   * @param language the subtitle language
   * @param source where the subtitle came from
   * @param fileId the OpenSubtitles file id, or null for local files
   * @return the saved subtitle object
   */
  public SubtitleEntity attach(
      String bucket,
      String keyPath,
      String subtitleKey,
      String language,
      SubtitleSource source,
      Integer fileId) {
    logger.debug("Attaching subtitle {} to {}", subtitleKey, keyPath);

    SubtitleEntity entity = subtitleRepository
        .findByBucketAndSubtitleKey(bucket, subtitleKey)
        .orElseGet(SubtitleEntity::new);
    entity.setBucket(bucket);
    entity.setKeyPath(keyPath);
    entity.setSubtitleKey(subtitleKey);
    entity.setLanguage(normaliseLanguage(language));
    entity.setSource(source == null ? SubtitleSource.LOCAL : source);
    entity.setOpenSubtitlesFileId(fileId);
    entity.setRemoteObject(remoteObjects(bucket, keyPath));
    return subtitleRepository.save(entity);
  }

  /**
   * Lists the subtitle objects attached to a video key.
   *
   * @param bucket the bucket holding the video
   * @param keyPath the video key
   * @return the attached subtitle objects
   */
  public List<SubtitleEntity> listForVideo(String bucket, String keyPath) {
    return subtitleRepository.findByBucketAndKeyPath(bucket, keyPath);
  }

  /**
   * Locates a subtitle object by its S3 key.
   *
   * @param bucket the bucket holding the object
   * @param subtitleKey the subtitle object key
   * @return the subtitle object when present
   */
  public Optional<SubtitleEntity> findBySubtitleKey(String bucket, String subtitleKey) {
    return subtitleRepository.findByBucketAndSubtitleKey(bucket, subtitleKey);
  }

  /**
   * Deletes the subtitle object for a subtitle key.
   *
   * @param bucket the bucket holding the object
   * @param subtitleKey the subtitle object key
   */
  public void deleteBySubtitleKey(String bucket, String subtitleKey) {
    logger.info("Deleting subtitle object {} in {}", subtitleKey, bucket);
    subtitleRepository.deleteBySubtitleKey(bucket, subtitleKey);
  }

  /**
   * Deletes every subtitle object attached to a video key.
   *
   * @param bucket the bucket holding the video
   * @param keyPath the video key
   */
  public void deleteForVideo(String bucket, String keyPath) {
    logger.info("Deleting subtitle objects for {} in {}", keyPath, bucket);
    subtitleRepository.deleteForVideo(bucket, keyPath);
  }

  /**
   * Deletes subtitle objects under a key prefix.
   *
   * @param bucket the bucket holding the objects
   * @param keyPrefix the prefix being removed
   */
  public void deleteUnderPrefix(String bucket, String keyPrefix) {
    logger.info("Deleting subtitle objects under {} in {}", keyPrefix, bucket);
    subtitleRepository.deleteUnderPrefix(bucket, prefixOf(keyPrefix));
  }

  /**
   * Moves subtitle references when a video key or folder is renamed.
   *
   * @param bucket the bucket holding the objects
   * @param sourceKey the original key or prefix
   * @param targetKey the new key or prefix
   */
  public void updatePath(String bucket, String sourceKey, String targetKey) {
    logger.info("Updating subtitle paths in {} from {} to {}", bucket, sourceKey, targetKey);

    String sourcePrefix = prefixOf(sourceKey);
    String targetPrefix = prefixOf(targetKey);
    List<SubtitleEntity> rows = new ArrayList<>(
        subtitleRepository.findByPrefix(bucket, sourcePrefix));

    if (!sourceKey.endsWith("/")) {
      for (SubtitleEntity exact : subtitleRepository.findByBucketAndKeyPath(bucket, sourceKey)) {
        if (rows.stream().noneMatch(row -> row.getId().equals(exact.getId()))) {
          rows.add(exact);
        }
      }
    }

    for (SubtitleEntity row : rows) {
      String keyPath = moveKey(row.getKeyPath(), sourceKey, targetKey, sourcePrefix, targetPrefix);
      if (keyPath == null) {
        continue;
      }
      String subtitleKey = moveKey(
          row.getSubtitleKey(), sourceKey, targetKey, sourcePrefix, targetPrefix);
      row.setKeyPath(keyPath);
      row.setSubtitleKey(subtitleKey == null ? row.getSubtitleKey() : subtitleKey);
      row.setRemoteObject(remoteObjects(bucket, keyPath));
      subtitleRepository.save(row);
    }
  }

  private static String moveKey(
      String key,
      String sourceKey,
      String targetKey,
      String sourcePrefix,
      String targetPrefix) {
    if (key.equals(sourceKey)) {
      return targetKey;
    }
    if (key.startsWith(sourcePrefix)) {
      return targetPrefix + key.substring(sourcePrefix.length());
    }
    return null;
  }

  private static String prefixOf(String key) {
    return key.endsWith("/") ? key : key + "/";
  }

  private static String normaliseLanguage(String language) {
    if (language == null || language.isBlank()) {
      return DEFAULT_LANGUAGE;
    }
    return language.trim().toLowerCase(Locale.ROOT);
  }

  private static List<String> remoteObjects(String bucket, String keyPath) {
    return List.of(REMOTE_TYPE, "dionysus:key:" + bucket + "/" + keyPath);
  }
}

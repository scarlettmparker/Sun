package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.codegen.types.KeyEntry;
import com.sun.dionysus.graphql.models.KeyDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Mapper for converting S3 list results into GraphQL KeyEntry types.
 * Enriches entries with metadata from KeyDetail records.
 */
@Component
public class KeyEntryMapper {

  private static final Logger logger = LoggerFactory.getLogger(KeyEntryMapper.class);

  /**
   * Maps an S3 common prefix to a directory-style KeyEntry.
   *
   * @param prefix the S3 CommonPrefix
   * @return the mapped KeyEntry with isDirectory=true
   */
  public KeyEntry mapDirectory(CommonPrefix prefix) {
    logger.debug("Mapping directory key entry {}", prefix.prefix());
    KeyEntry entry = new KeyEntry();
    entry.setKey(prefix.prefix());
    entry.setIsDirectory(true);
    entry.setSize(0);
    return entry;
  }

  /**
   * Maps an S3 common prefix to a directory-style KeyEntry with optional KeyDetail metadata.
   *
   * @param prefix the S3 CommonPrefix
   * @param keyDetail optional KeyDetail with metadata
   * @return the mapped KeyEntry with name/description from KeyDetail if available
   */
  public KeyEntry mapDirectory(CommonPrefix prefix, KeyDetail keyDetail) {
    logger.debug("Mapping directory key entry {}", prefix.prefix());
    KeyEntry entry = new KeyEntry();
    entry.setKey(prefix.prefix());
    entry.setIsDirectory(true);
    entry.setSize(0);
    if (keyDetail != null) {
      entry.setName(keyDetail.getName());
      entry.setDescription(keyDetail.getDescription());
    }
    return entry;
  }

  /**
   * Maps an S3 object to a file KeyEntry.
   *
   * @param object the S3Object
   * @return the mapped KeyEntry with isDirectory=false
   */
  public KeyEntry mapFile(S3Object object) {
    logger.debug("Mapping file key entry {}", object.key());
    KeyEntry entry = new KeyEntry();
    entry.setKey(object.key());
    entry.setIsDirectory(false);
    entry.setSize(object.size() != null ? object.size().intValue() : 0);
    if (object.lastModified() != null) {
      entry.setLastModified(object.lastModified().toString());
    }
    return entry;
  }

  /**
   * Maps an S3 object to a file KeyEntry with optional KeyDetail metadata.
   *
   * @param object the S3Object
   * @param keyDetail optional KeyDetail with metadata
   * @return the mapped KeyEntry with name/description from KeyDetail if available
   */
  public KeyEntry mapFile(S3Object object, KeyDetail keyDetail) {
    logger.debug("Mapping file key entry {}", object.key());
    KeyEntry entry = new KeyEntry();
    entry.setKey(object.key());
    entry.setIsDirectory(false);
    entry.setSize(object.size() != null ? object.size().intValue() : 0);
    if (object.lastModified() != null) {
      entry.setLastModified(object.lastModified().toString());
    }
    if (keyDetail != null) {
      entry.setName(keyDetail.getName());
      entry.setDescription(keyDetail.getDescription());
    }
    return entry;
  }
}

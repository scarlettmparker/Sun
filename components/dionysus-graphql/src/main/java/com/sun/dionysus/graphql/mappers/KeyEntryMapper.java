package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.codegen.types.KeyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Mapper for converting S3 list results into GraphQL KeyEntry types.
 */
@Component
public class KeyEntryMapper {

  private static final Logger logger = LoggerFactory.getLogger(KeyEntryMapper.class);

  /**
   * Map an S3 common prefix to a directory-style KeyEntry.
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
   * Map an S3 object to a file KeyEntry.
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
}

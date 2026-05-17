package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.codegen.types.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Mapper for converting S3 objects into GraphQL File types.
 */
@Component
public class FileMapper {

  private static final Logger logger = LoggerFactory.getLogger(FileMapper.class);

  public File mapObject(S3Object object) {
    logger.debug("Mapping S3 object to File: {}", object.key());
    File entry = new File();
    entry.setKey(object.key());
    entry.setSize(object.size() != null ? object.size().intValue() : 0);
    if (object.lastModified() != null) {
      entry.setLastModified(object.lastModified().toString());
    }
    return entry;
  }
}

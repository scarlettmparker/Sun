package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.graphql.models.KeyDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting KeyDetail domain entities to GraphQL representations.
 */
@Component
public class KeyDetailMapper {

  private static final Logger logger = LoggerFactory.getLogger(KeyDetailMapper.class);

  /**
   * Maps a KeyDetail entity to name and description for GraphQL types.
   *
   * @param keyDetail the KeyDetail entity to extract metadata from
   * @return the KeyDetail entity (used to access name and description fields)
   */
  public KeyDetail map(KeyDetail keyDetail) {
    if (keyDetail == null) {
      logger.debug("Mapping null KeyDetail");
      return null;
    }

    logger.debug("Mapping key detail for path: {} in bucket: {}", keyDetail.getKeyPath(), keyDetail.getBucket());
    return keyDetail;
  }
}

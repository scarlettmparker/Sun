package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.codegen.types.KeyDetail;
import com.sun.dionysus.graphql.models.KeyDetailEntity;
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
   * Maps a KeyDetailEntity entity to the GraphQL KeyDetail type.
   *
   * @param keyDetailEntity the KeyDetailEntity entity to map from
   * @return the mapped KeyDetail GraphQL type, or null if input is null
   */
  public KeyDetail map(KeyDetailEntity keyDetailEntity) {
    if (keyDetailEntity == null) {
      logger.debug("Mapping null KeyDetailEntity");
      return null;
    }

    logger.debug("Mapping key detail entity for path: {} in bucket: {}", keyDetailEntity.getKeyPath(), keyDetailEntity.getBucket());

    KeyDetail keyDetail = new KeyDetail();
    keyDetail.setId(keyDetailEntity.getId().toString());
    keyDetail.setBucket(keyDetailEntity.getBucket());
    keyDetail.setKeyPath(keyDetailEntity.getKeyPath());
    keyDetail.setName(keyDetailEntity.getName());
    keyDetail.setDescription(keyDetailEntity.getDescription());
    keyDetail.setStatus(keyDetailEntity.getStatus().name());
    keyDetail.setContentType(keyDetailEntity.getContentType());
    if (keyDetailEntity.getCreatedAt() != null) {
      keyDetail.setCreatedAt(keyDetailEntity.getCreatedAt().toString());
    }
    if (keyDetailEntity.getLastUpdatedAt() != null) {
      keyDetail.setLastUpdatedAt(keyDetailEntity.getLastUpdatedAt().toString());
    }
    if (keyDetailEntity.getArchivedAt() != null) {
      keyDetail.setArchivedAt(keyDetailEntity.getArchivedAt().toString());
    }
    return keyDetail;
  }
}
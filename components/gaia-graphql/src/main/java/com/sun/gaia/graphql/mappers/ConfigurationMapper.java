package com.sun.gaia.graphql.mappers;

import java.time.ZoneOffset;
import com.sun.gaia.codegen.types.Configuration;
import com.sun.gaia.model.ConfigurationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting configuration entities to GraphQL types.
 */
@Component
public class ConfigurationMapper {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationMapper.class);

  /**
   * Maps a ConfigurationEntity to a GraphQL Configuration type.
   *
   * @param entity the domain entity to map
   * @return the mapped GraphQL Configuration type
   */
  public Configuration map(ConfigurationEntity entity) {
    logger.debug("Mapping configuration {}", entity.getName());

    Configuration configuration = Configuration.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName())
        .description(entity.getDescription())
        .enabled(entity.isEnabled())
        .content(entity.getContent())
        .lastAppliedAt(entity.getLastAppliedAt() == null ? null : entity.getLastAppliedAt().atOffset(ZoneOffset.UTC))
        .lastApplyError(entity.getLastApplyError())
        .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC))
        .updatedAt(entity.getLastUpdatedAt() == null ? null : entity.getLastUpdatedAt().atOffset(ZoneOffset.UTC))
        .build();

    logger.debug("Mapped configuration {}", configuration.getId());
    return configuration;
  }
}

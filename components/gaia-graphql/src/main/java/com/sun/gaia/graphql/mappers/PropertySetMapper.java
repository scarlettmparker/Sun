package com.sun.gaia.graphql.mappers;

import com.sun.gaia.codegen.types.PropertySetEntry;
import com.sun.gaia.codegen.types.PropertySetSchema;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.model.PropertySetSchemaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting property-set entities to GraphQL types.
 */
@Component
public class PropertySetMapper {

  private static final Logger logger = LoggerFactory.getLogger(PropertySetMapper.class);

  /**
   * Maps a PropertySetEntryEntity to a GraphQL PropertySetEntry type.
   *
   * @param entity the domain entity to map
   * @return the mapped GraphQL PropertySetEntry type
   */
  public PropertySetEntry map(PropertySetEntryEntity entity) {
    logger.debug("Mapping property set entry {}/{}/{}", entity.getOwnerKey(),
        entity.getPropertySet(), entity.getEntryName());

    PropertySetEntry entry = PropertySetEntry.newBuilder()
        .id(entity.getId().toString())
        .ownerKey(entity.getOwnerKey())
        .propertySet(entity.getPropertySet())
        .entryName(entity.getEntryName())
        .values(entity.getValues())
        .configurable(entity.isConfigurable())
        .status(entity.getStatus())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();

    logger.debug("Mapped property set entry {}", entry.getId());
    return entry;
  }

  /**
   * Maps a PropertySetSchemaEntity to a GraphQL PropertySetSchema type.
   *
   * @param entity the domain entity to map
   * @return the mapped GraphQL PropertySetSchema type
   */
  public PropertySetSchema map(PropertySetSchemaEntity entity) {
    logger.debug("Mapping property set schema {}/{}", entity.getOwnerKey(), entity.getName());

    PropertySetSchema schema = PropertySetSchema.newBuilder()
        .id(entity.getId().toString())
        .ownerKey(entity.getOwnerKey())
        .name(entity.getName())
        .properties(entity.getProperties())
        .configurable(entity.isConfigurable())
        .status(entity.getStatus())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();

    logger.debug("Mapped property set schema {}", schema.getId());
    return schema;
  }
}

package com.sun.briareus.graphql.mappers;

import com.sun.briareus.codegen.types.BlogPropertySet;
import com.sun.briareus.codegen.types.BlogPropertySetSchema;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.model.PropertySetSchemaEntity;
import org.springframework.stereotype.Component;

/**
 * Maps property set entities.
 */
@Component
public class BlogPropertySetMapper {

  /**
   * Maps a schema entity to GraphQL.
   *
   * @param entity the schema entity
   * @return the mapped GraphQL type
   */
  public BlogPropertySetSchema mapSchema(PropertySetSchemaEntity entity) {
    return BlogPropertySetSchema.newBuilder()
        .id(entity.getId().toString())
        .ownerKey(entity.getOwnerKey())
        .name(entity.getName())
        .properties(entity.getProperties())
        .build();
  }

  /**
   * Maps a schema and entry to a property set payload.
   *
   * @param schema the schema entity
   * @param entry the entry entity, or null when absent
   * @param entryName the entry name
   * @return the payload
   */
  public BlogPropertySet map(PropertySetSchemaEntity schema, PropertySetEntryEntity entry, String entryName) {
    BlogPropertySetSchema mappedSchema = mapSchema(schema);
    Object values = entry == null ? null : entry.getValues();
    return BlogPropertySet.newBuilder()
        .schema(mappedSchema)
        .values(values)
        .entryName(entryName)
        .build();
  }
}

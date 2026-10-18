package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.error.MutationException;
import com.sun.gaia.codegen.types.DeletePropertyEntryResponse;
import com.sun.gaia.codegen.types.PropertySetEntry;
import com.sun.gaia.codegen.types.PropertySetSchema;
import com.sun.gaia.codegen.types.PropertySetSchemaInput;
import com.sun.gaia.codegen.types.RegisterPropertySetSchemaResponse;
import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.codegen.types.SetPropertyInput;
import com.sun.gaia.codegen.types.SetPropertyResponse;
import com.sun.gaia.codegen.types.UpsertPropertyEntryInput;
import com.sun.gaia.codegen.types.UpsertPropertyEntryResponse;
import com.sun.gaia.graphql.mappers.PropertySetMapper;
import com.sun.gaia.graphql.services.support.GaiaGraphQLSupport;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.service.PropertySetService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for property-set operations.
 */
@DgsComponent
public class PropertySetDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(PropertySetDataFetcher.class);

  private final PropertySetService propertySetService;
  private final PropertySetMapper propertySetMapper;

  public PropertySetDataFetcher(
      PropertySetService propertySetService,
      PropertySetMapper propertySetMapper) {
    this.propertySetService = propertySetService;
    this.propertySetMapper = propertySetMapper;
  }

  /**
   * Returns one entry's values, or every active entry mapped by name.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @param entry the entry name, or null for all entries
   * @return the values map, or null when a named entry is missing
   */
  @DgsData(parentType = "GaiaQueries", field = "propertySet")
  @PreAuthorize("permitAll()")
  @Transactional(readOnly = true)
  public Object propertySet(String ownerKey, String name, String entry) {
    if (entry == null) {
      Map<String, Object> all = new LinkedHashMap<>();
      for (PropertySetEntryEntity entity : propertySetService.listActiveEntries(ownerKey, name)) {
        all.put(entity.getEntryName(), entity.getValues());
      }
      return all;
    }
    return propertySetService.getEntry(ownerKey, name, entry)
        .map(PropertySetEntryEntity::getValues)
        .orElse(null);
  }

  /**
   * Lists all active entries in a property set.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @return the entries
   */
  @DgsData(parentType = "GaiaQueries", field = "propertySets")
  @PreAuthorize("permitAll()")
  @Transactional(readOnly = true)
  public List<PropertySetEntry> propertySets(String ownerKey, String name) {
    return propertySetService.listActiveEntries(ownerKey, name).stream()
        .map(propertySetMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Locates the schema for a property set.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @return the schema, or null when absent
   */
  @DgsData(parentType = "GaiaQueries", field = "propertySetSchema")
  @PreAuthorize("permitAll()")
  @Transactional(readOnly = true)
  public PropertySetSchema propertySetSchema(String ownerKey, String name) {
    return propertySetService.getSchemaEntity(ownerKey, name)
        .map(propertySetMapper::map)
        .orElse(null);
  }

  /**
   * Lists schemas for an owner, or all owners when null.
   *
   * @param ownerKey the owner key, or null for all
   * @return the schemas
   */
  @DgsData(parentType = "GaiaQueries", field = "propertySetSchemas")
  @PreAuthorize("permitAll()")
  @Transactional(readOnly = true)
  public List<PropertySetSchema> propertySetSchemas(String ownerKey) {
    return propertySetService.listSchemas(ownerKey).stream()
        .map(propertySetMapper::map)
        .collect(Collectors.toList());
  }

   /**
    * Property-set entries the remote user may execute.
    */
  @DgsData(parentType = "GaiaQueries", field = "accessibleCommandIntents")
  @PreAuthorize("@permissions.has('graphql.gaia.accessibleCommandIntents')")
  @Transactional(readOnly = true)
  public List<PropertySetEntry> accessibleCommandIntents(
      RemoteUserType remoteUserType, String remoteUserId,
      String ownerKey, String propertySet) {
    if (remoteUserType != RemoteUserType.DISCORD || remoteUserId == null || remoteUserId.isBlank()) {
      return List.of();
    }
    return propertySetService
        .listAccessibleEntries(remoteUserId, ownerKey, propertySet)
        .stream()
        .map(propertySetMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Creates or replaces a property-set entry.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @param entry the entry name
   * @param input the values to store
   * @return the saved entry
   */
  @DgsData(parentType = "GaiaMutations", field = "upsertPropertyEntry")
  @PreAuthorize("@permissions.has('graphql.gaia.upsertPropertyEntry')")
  @Transactional
  public UpsertPropertyEntryResponse upsertPropertyEntry(String ownerKey, String name, String entry,
      UpsertPropertyEntryInput input) {
    PropertySetEntry saved = propertySetMapper.map(
        propertySetService.upsertEntry(ownerKey, name, entry, GaiaGraphQLSupport.asMap(input.getValues()), false));
    return UpsertPropertyEntryResponse.newBuilder()
        .message("Property entry upserted")
        .entry(saved)
        .build();
  }

  /**
   * Sets a single property on an entry.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @param entry the entry name
   * @param input the property and value
   * @return the saved entry
   */
  @DgsData(parentType = "GaiaMutations", field = "setProperty")
  @PreAuthorize("@permissions.has('graphql.gaia.setProperty')")
  @Transactional
  public SetPropertyResponse setProperty(String ownerKey, String name, String entry,
      SetPropertyInput input) {
    PropertySetEntry saved = propertySetMapper.map(
        propertySetService.setProperty(ownerKey, name, entry, input.getProperty(), input.getValue()));
    return SetPropertyResponse.newBuilder()
        .message("Property updated")
        .entry(saved)
        .build();
  }

  /**
   * Registers a property-set schema.
   *
   * @param input the schema input
   * @return the saved schema
   */
  @DgsData(parentType = "GaiaMutations", field = "registerPropertySetSchema")
  @PreAuthorize("@permissions.has('graphql.gaia.registerPropertySetSchema')")
  @Transactional
  public RegisterPropertySetSchemaResponse registerPropertySetSchema(PropertySetSchemaInput input) {
    PropertySetSchema schema = propertySetMapper.map(propertySetService.upsertSchema(
        input.getOwnerKey(), input.getName(),
        input.getConfigurable() != null && input.getConfigurable(),
        GaiaGraphQLSupport.asMap(input.getProperties())));
    return RegisterPropertySetSchemaResponse.newBuilder()
        .message("Property set schema registered")
        .schema(schema)
        .build();
  }

  /**
   * Deletes a property-set entry.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @param entry the entry name
   * @return the result
   */
  @DgsData(parentType = "GaiaMutations", field = "deletePropertyEntry")
  @PreAuthorize("@permissions.has('graphql.gaia.deletePropertyEntry')")
  @Transactional
  public DeletePropertyEntryResponse deletePropertyEntry(String ownerKey, String name, String entry) {
    boolean deleted = propertySetService.deleteEntry(ownerKey, name, entry);
    if (deleted) {
      return DeletePropertyEntryResponse.newBuilder()
          .message("Entry deleted")
          .id(entry)
          .build();
    }
    throw new MutationException("Entry not found");
  }
}

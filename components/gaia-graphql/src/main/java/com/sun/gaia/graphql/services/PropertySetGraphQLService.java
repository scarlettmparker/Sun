package com.sun.gaia.graphql.services;

import com.sun.gaia.codegen.types.PropertySetEntry;
import com.sun.gaia.codegen.types.PropertySetSchema;
import com.sun.gaia.codegen.types.PropertySetSchemaInput;
import com.sun.gaia.codegen.types.RemoteUserType;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for property sets.
 */
@Service
public class PropertySetGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(PropertySetGraphQLService.class);

  private final PropertySetService propertySetService;
  private final PropertySetMapper propertySetMapper;

  public PropertySetGraphQLService(
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
  @Transactional(readOnly = true)
  public PropertySetSchema propertySetSchema(String ownerKey, String name) {
    return propertySetService.getSchemaEntity(ownerKey, name)
        .map(propertySetMapper::map)
        .orElse(null);
  }

  /**
   * Creates or replaces a property-set entry.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @param entry the entry name
   * @param values the values to store
   * @return the saved entry
   */
  @Transactional
  public PropertySetEntry upsertPropertyEntry(String ownerKey, String name, String entry,
      Object values) {
    return propertySetMapper.map(
        propertySetService.upsertEntry(ownerKey, name, entry, GaiaGraphQLSupport.asMap(values), false));
  }

  /**
   * Sets a single property on an entry.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @param entry the entry name
   * @param property the property name
   * @param value the property value
   * @return the saved entry
   */
  @Transactional
  public PropertySetEntry setProperty(String ownerKey, String name, String entry, String property,
      Object value) {
    return propertySetMapper.map(
        propertySetService.setProperty(ownerKey, name, entry, property, value));
  }

  /**
   * Registers a property-set schema.
   *
   * @param input the schema input
   * @return the saved schema
   */
  @Transactional
  public PropertySetSchema registerPropertySetSchema(PropertySetSchemaInput input) {
    return propertySetMapper.map(propertySetService.upsertSchema(
        input.getOwnerKey(), input.getName(),
        input.getConfigurable() != null && input.getConfigurable(),
        GaiaGraphQLSupport.asMap(input.getProperties())));
  }

  /**
   * Property-set entries the remote user may execute.
   *
   * @param remoteUserType the remote identity type
   * @param remoteUserId the remote identity id
   * @param ownerKey the property-set owner
   * @param propertySet the property-set name
   * @return the accessible entries
   */
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
}

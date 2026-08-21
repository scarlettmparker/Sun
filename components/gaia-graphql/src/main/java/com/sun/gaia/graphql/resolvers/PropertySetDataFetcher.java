package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.PropertySetEntry;
import com.sun.gaia.codegen.types.PropertySetSchema;
import com.sun.gaia.codegen.types.PropertySetSchemaInput;
import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.graphql.services.PropertySetGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for property-set operations.
 */
@DgsComponent
public class PropertySetDataFetcher {

  private final PropertySetGraphQLService propertySetGraphQLService;

  public PropertySetDataFetcher(PropertySetGraphQLService propertySetGraphQLService) {
    this.propertySetGraphQLService = propertySetGraphQLService;
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
  public Object propertySet(String ownerKey, String name, String entry) {
    return propertySetGraphQLService.propertySet(ownerKey, name, entry);
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
  public List<PropertySetEntry> propertySets(String ownerKey, String name) {
    return propertySetGraphQLService.propertySets(ownerKey, name);
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
  public PropertySetSchema propertySetSchema(String ownerKey, String name) {
    return propertySetGraphQLService.propertySetSchema(ownerKey, name);
  }

   /**
    * Property-set entries the remote user may execute.
    */
  @DgsData(parentType = "GaiaQueries", field = "accessibleCommandIntents")
  @PreAuthorize("@permissions.has('graphql.gaia.accessibleCommandIntents')")
  public List<PropertySetEntry> accessibleCommandIntents(
      RemoteUserType remoteUserType, String remoteUserId,
      String ownerKey, String propertySet) {
    return propertySetGraphQLService.accessibleCommandIntents(
        remoteUserType, remoteUserId, ownerKey, propertySet);
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
  @DgsData(parentType = "GaiaMutations", field = "upsertPropertyEntry")
  @PreAuthorize("@permissions.has('graphql.gaia.upsertPropertyEntry')")
  public PropertySetEntry upsertPropertyEntry(String ownerKey, String name, String entry,
      Object values) {
    return propertySetGraphQLService.upsertPropertyEntry(ownerKey, name, entry, values);
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
  @DgsData(parentType = "GaiaMutations", field = "setProperty")
  @PreAuthorize("@permissions.has('graphql.gaia.setProperty')")
  public PropertySetEntry setProperty(String ownerKey, String name, String entry, String property,
      Object value) {
    return propertySetGraphQLService.setProperty(ownerKey, name, entry, property, value);
  }

  /**
   * Registers a property-set schema.
   *
   * @param input the schema input
   * @return the saved schema
   */
  @DgsData(parentType = "GaiaMutations", field = "registerPropertySetSchema")
  @PreAuthorize("@permissions.has('graphql.gaia.registerPropertySetSchema')")
  public PropertySetSchema registerPropertySetSchema(PropertySetSchemaInput input) {
    return propertySetGraphQLService.registerPropertySetSchema(input);
  }
}

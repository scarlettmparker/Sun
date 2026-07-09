package com.sun.gaia.service;

import com.sun.base.service.BaseService;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.model.PropertySetSchemaEntity;
import com.sun.gaia.model.enums.EntryStatus;
import com.sun.gaia.repository.PropertySetEntryRepository;
import com.sun.gaia.repository.PropertySetSchemaRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Reads and writes schema-constrained property-set entries.
 */
@Service
public class PropertySetService extends BaseService<PropertySetEntryEntity> {

  private final PropertySetEntryRepository entryRepository;
  private final PropertySetSchemaRepository schemaRepository;
  private final PropertySetValidator validator;

  public PropertySetService(PropertySetEntryRepository entryRepository,
      PropertySetSchemaRepository schemaRepository, PropertySetValidator validator) {
    super(entryRepository);
    this.entryRepository = entryRepository;
    this.schemaRepository = schemaRepository;
    this.validator = validator;
  }

  /**
   * Locates a single active entry.
   *
   * @param ownerKey the owner key
   * @param propertySet the property set name
   * @param entryName the entry name
   * @return the entry if present
   */
  public Optional<PropertySetEntryEntity> getEntry(String ownerKey, String propertySet,
      String entryName) {
    return entryRepository
        .findByOwnerKeyAndPropertySetAndEntryName(ownerKey, propertySet, entryName)
        .filter(entry -> entry.getStatus() == EntryStatus.ACTIVE);
  }

  /**
   * Lists all active entries in a property set.
   *
   * @param ownerKey the owner key
   * @param propertySet the property set name
   * @return the active entries
   */
  public List<PropertySetEntryEntity> listActiveEntries(String ownerKey, String propertySet) {
    return entryRepository.findByOwnerKeyAndPropertySetAndStatus(
        ownerKey, propertySet, EntryStatus.ACTIVE);
  }

  /**
   * Locates the schema for a property set, preferring an owner-scoped schema and
   * falling back to a global one.
   *
   * @param ownerKey the owner key
   * @param propertySet the property set name
   * @return the schema if present
   */
  public Optional<PropertySetSchemaEntity> getSchemaEntity(String ownerKey, String propertySet) {
    return schemaRepository.findByOwnerKeyAndName(ownerKey, propertySet)
        .or(() -> schemaRepository.findByOwnerKeyAndName(null, propertySet));
  }

  /**
   * Creates or replaces a property-set schema.
   *
   * @param ownerKey the owner key
   * @param name the property set name
   * @param configurable whether the schema is owned by the config loader
   * @param properties the property definitions
   * @return the saved schema
   */
  public PropertySetSchemaEntity upsertSchema(String ownerKey, String name, boolean configurable,
      Map<String, Object> properties) {
    PropertySetSchemaEntity schema = schemaRepository.findByOwnerKeyAndName(ownerKey, name)
        .orElseGet(() -> {
          PropertySetSchemaEntity created = new PropertySetSchemaEntity();
          created.setOwnerKey(ownerKey);
          created.setName(name);
          return created;
        });
    schema.setProperties(properties);
    schema.setConfigurable(configurable);
    schema.setStatus(EntryStatus.ACTIVE);
    return schemaRepository.save(schema);
  }

  /**
   * Creates or replaces an entry, validating its values against the schema.
   *
   * @param ownerKey the owner key
   * @param propertySet the property set name
   * @param entryName the entry name
   * @param values the values to store
   * @param configurable whether the entry is owned by the config loader
   * @return the saved entry
   */
  public PropertySetEntryEntity upsertEntry(String ownerKey, String propertySet, String entryName,
      Map<String, Object> values, boolean configurable) {
    validateAgainstSchema(ownerKey, propertySet, values);
    PropertySetEntryEntity entry = entryRepository
        .findByOwnerKeyAndPropertySetAndEntryName(ownerKey, propertySet, entryName)
        .orElseGet(() -> {
          PropertySetEntryEntity created = new PropertySetEntryEntity();
          created.setOwnerKey(ownerKey);
          created.setPropertySet(propertySet);
          created.setEntryName(entryName);
          return created;
        });
    entry.setValues(values);
    entry.setConfigurable(configurable);
    entry.setStatus(EntryStatus.ACTIVE);
    return save(entry);
  }

  /**
   * Sets a single property on an entry, creating the entry when needed.
   *
   * @param ownerKey the owner key
   * @param propertySet the property set name
   * @param entryName the entry name
   * @param property the property name
   * @param value the property value
   * @return the saved entry
   */
  public PropertySetEntryEntity setProperty(String ownerKey, String propertySet, String entryName,
      String property, Object value) {
    PropertySetEntryEntity entry = entryRepository
        .findByOwnerKeyAndPropertySetAndEntryName(ownerKey, propertySet, entryName)
        .orElseGet(() -> {
          PropertySetEntryEntity created = new PropertySetEntryEntity();
          created.setOwnerKey(ownerKey);
          created.setPropertySet(propertySet);
          created.setEntryName(entryName);
          created.setValues(new LinkedHashMap<>());
          return created;
        });
    Map<String, Object> values = new LinkedHashMap<>(
        entry.getValues() != null ? entry.getValues() : Map.of());
    values.put(property, value);
    validateAgainstSchema(ownerKey, propertySet, values);
    entry.setValues(values);
    entry.setStatus(EntryStatus.ACTIVE);
    return save(entry);
  }

  /**
   * Validates values against the schema for a property set, if a schema exists.
   *
   * @param ownerKey the owner key
   * @param propertySet the property set name
   * @param values the values to validate
   */
  private void validateAgainstSchema(String ownerKey, String propertySet,
      Map<String, Object> values) {
    getSchemaEntity(ownerKey, propertySet)
        .ifPresent(schema -> validator.validate(schema.getProperties(), values));
  }
}

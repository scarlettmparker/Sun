package com.sun.gaia.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.gaia.model.ConfigurationEntity;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.model.PropertySetSchemaEntity;
import com.sun.gaia.model.enums.EntryStatus;
import com.sun.gaia.repository.ConfigurationRepository;
import com.sun.gaia.repository.PropertySetEntryRepository;
import com.sun.gaia.repository.PropertySetSchemaRepository;
import com.sun.gaia.service.config.ConfigurationContent;
import com.sun.gaia.service.config.ConfigurationContent.PropertySet;
import com.sun.gaia.service.config.ConfigurationContent.Schema;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materialises a configuration's desired state into live data.
 */
@Component
public class ConfigurationReconciler {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationReconciler.class);

  private final ConfigurationRepository configurationRepository;
  private final AccountService accountService;
  private final PropertySetSchemaRepository schemaRepository;
  private final PropertySetEntryRepository entryRepository;
  private final PropertySetValidator validator;
  private final ObjectMapper objectMapper;

  public ConfigurationReconciler(ConfigurationRepository configurationRepository,
      AccountService accountService, PropertySetSchemaRepository schemaRepository,
      PropertySetEntryRepository entryRepository, PropertySetValidator validator,
      ObjectMapper objectMapper) {
    this.configurationRepository = configurationRepository;
    this.accountService = accountService;
    this.schemaRepository = schemaRepository;
    this.entryRepository = entryRepository;
    this.validator = validator;
    this.objectMapper = objectMapper;
  }

  /**
   * Reconciles every enabled configuration.
   */
  @Transactional
  public void reconcileAll() {
    List<ConfigurationEntity> configs = configurationRepository.findByEnabledTrue();
    logger.info("Reconciling {} enabled configuration(s)", configs.size());
    for (ConfigurationEntity config : configs) {
      reconcile(config);
    }
  }

  /**
   * Reconciles a single configuration by id.
   *
   * @param id the configuration id
   * @return the reconciled configuration
   */
  @Transactional
  public ConfigurationEntity reconcileById(UUID id) {
    ConfigurationEntity config = configurationRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + id));
    return reconcile(config);
  }

  /**
   * Applies a configuration's desired state.
   *
   * @param config the configuration to apply
   * @return the reconciled configuration
   */
  @Transactional
  public ConfigurationEntity reconcile(ConfigurationEntity config) {
    try {
      ConfigurationContent content = objectMapper.convertValue(config.getContent(),
          ConfigurationContent.class);
      if (content != null) {
        reconcileUsers(content);
        reconcileSchemas(content);
        reconcilePropertySets(content);
      }
      config.setLastAppliedAt(LocalDateTime.now());
      config.setLastApplyError(null);
      logger.info("Reconciled configuration {}", config.getName());
    } catch (Exception e) {
      config.setLastApplyError(e.getMessage());
      logger.error("Failed to reconcile configuration {}", config.getName(), e);
    }
    return configurationRepository.save(config);
  }

  /**
   * Ensures a ghost account exists for each declared user.
   *
   * @param content the parsed configuration
   */
  private void reconcileUsers(ConfigurationContent content) {
    if (content.users() == null) {
      return;
    }
    for (ConfigurationContent.User user : content.users()) {
      accountService.upsertGhostAccount(user.key());
    }
  }

  /**
   * Upserts declared schemas and archives configurable schemas no longer present.
   *
   * @param content the parsed configuration
   */
  private void reconcileSchemas(ConfigurationContent content) {
    if (content.schemas() == null) {
      return;
    }
    Map<String, Set<String>> declaredByOwner = new java.util.HashMap<>();
    for (Schema schema : content.schemas()) {
      PropertySetSchemaEntity entity = schemaRepository
          .findByOwnerKeyAndName(schema.ownerKey(), schema.name())
          .orElseGet(() -> {
            PropertySetSchemaEntity created = new PropertySetSchemaEntity();
            created.setOwnerKey(schema.ownerKey());
            created.setName(schema.name());
            return created;
          });
      entity.setProperties(schema.properties());
      entity.setConfigurable(schema.configurable());
      entity.setStatus(EntryStatus.ACTIVE);
      schemaRepository.save(entity);
      declaredByOwner.computeIfAbsent(schema.ownerKey(), k -> new HashSet<>())
          .add(schema.name());
    }
    for (Map.Entry<String, Set<String>> entry : declaredByOwner.entrySet()) {
      archiveRemovedSchemas(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Archives configurable schemas for an owner that are no longer declared.
   *
   * @param ownerKey the owner key
   * @param declaredNames the schema names still declared
   */
  private void archiveRemovedSchemas(String ownerKey, Set<String> declaredNames) {
    for (PropertySetSchemaEntity schema : schemaRepository.findByOwnerKeyAndConfigurableTrue(
        ownerKey)) {
      if (!declaredNames.contains(schema.getName())) {
        schema.setStatus(EntryStatus.ARCHIVED);
        schemaRepository.save(schema);
      }
    }
  }

  /**
   * Upserts declared entries and archives configurable entries no longer present.
   *
   * @param content the parsed configuration
   */
  private void reconcilePropertySets(ConfigurationContent content) {
    if (content.propertySets() == null) {
      return;
    }
    for (PropertySet propertySet : content.propertySets()) {
      PropertySetSchemaEntity schema = schemaRepository
          .findByOwnerKeyAndName(propertySet.ownerKey(), propertySet.name()).orElse(null);
      Set<String> declaredEntries = new HashSet<>();
      if (propertySet.entries() != null) {
        for (ConfigurationContent.Entry entry : propertySet.entries()) {
          if (schema != null) {
            validator.validate(schema.getProperties(), entry.values());
          }
          upsertEntry(propertySet, entry);
          declaredEntries.add(entry.name());
        }
      }
      archiveRemovedEntries(propertySet, declaredEntries);
    }
  }

  /**
   * Creates or replaces an entry from a declared property set.
   *
   * @param propertySet the declared property set
   * @param entry the declared entry
   */
  private void upsertEntry(PropertySet propertySet, ConfigurationContent.Entry entry) {
    PropertySetEntryEntity entity = entryRepository
        .findByOwnerKeyAndPropertySetAndEntryName(propertySet.ownerKey(), propertySet.name(),
            entry.name())
        .orElseGet(() -> {
          PropertySetEntryEntity created = new PropertySetEntryEntity();
          created.setOwnerKey(propertySet.ownerKey());
          created.setPropertySet(propertySet.name());
          created.setEntryName(entry.name());
          return created;
        });
    entity.setValues(entry.values());
    entity.setConfigurable(propertySet.configurable());
    entity.setStatus(EntryStatus.ACTIVE);
    entryRepository.save(entity);
  }

  /**
   * Archives configurable entries no longer declared for a property set.
   *
   * @param propertySet the declared property set
   * @param declaredEntries the entry names still declared
   */
  private void archiveRemovedEntries(PropertySet propertySet, Set<String> declaredEntries) {
    List<PropertySetEntryEntity> configurable = entryRepository
        .findByOwnerKeyAndPropertySetAndConfigurable(propertySet.ownerKey(), propertySet.name(),
            true);
    for (PropertySetEntryEntity entry : configurable) {
      if (!declaredEntries.contains(entry.getEntryName())) {
        entry.setStatus(EntryStatus.ARCHIVED);
        entryRepository.save(entry);
      }
    }
  }
}

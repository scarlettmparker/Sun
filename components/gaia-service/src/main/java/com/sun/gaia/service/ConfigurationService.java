package com.sun.gaia.service;

import com.sun.base.service.BaseService;
import com.sun.gaia.model.ConfigurationEntity;
import com.sun.gaia.repository.ConfigurationRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * CRUD operations for stored configurations.
 */
@Service
public class ConfigurationService extends BaseService<ConfigurationEntity> {

  private final ConfigurationRepository configurationRepository;

  public ConfigurationService(ConfigurationRepository configurationRepository) {
    super(configurationRepository);
    this.configurationRepository = configurationRepository;
  }

  /**
   * Lists all configurations.
   *
   * @return the configurations
   */
  public List<ConfigurationEntity> list() {
    return findAll();
  }

  /**
   * Locates a configuration by id.
   *
   * @param id the configuration id
   * @return the configuration if present
   */
  public Optional<ConfigurationEntity> locate(java.util.UUID id) {
    return findById(id);
  }

  /**
   * Creates a new configuration.
   *
   * @param name the configuration name
   * @param description the description
   * @param enabled whether the configuration is active
   * @param content the desired-state document
   * @return the saved configuration
   */
  public ConfigurationEntity create(String name, String description, boolean enabled,
      Map<String, Object> content) {
    ConfigurationEntity entity = new ConfigurationEntity();
    entity.setName(name);
    entity.setDescription(description);
    entity.setEnabled(enabled);
    entity.setContent(content);
    return save(entity);
  }

  /**
   * Updates an existing configuration.
   *
   * @param id the configuration id
   * @param name the configuration name
   * @param description the description
   * @param enabled whether the configuration is active
   * @param content the desired-state document
   * @return the saved configuration
   */
  public ConfigurationEntity update(java.util.UUID id, String name, String description,
      boolean enabled, Map<String, Object> content) {
    ConfigurationEntity entity = findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + id));
    entity.setName(name);
    entity.setDescription(description);
    entity.setEnabled(enabled);
    entity.setContent(content);
    return save(entity);
  }
}

package com.sun.gaia.graphql.services;

import com.sun.gaia.codegen.types.Configuration;
import com.sun.gaia.codegen.types.ConfigurationInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.graphql.mappers.ConfigurationMapper;
import com.sun.gaia.graphql.services.support.GaiaGraphQLSupport;
import com.sun.gaia.service.ConfigurationReconciler;
import com.sun.gaia.service.ConfigurationService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for configurations.
 */
@Service
public class ConfigurationGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationGraphQLService.class);

  private final ConfigurationService configurationService;
  private final ConfigurationReconciler configurationReconciler;
  private final ConfigurationMapper configurationMapper;

  public ConfigurationGraphQLService(
      ConfigurationService configurationService,
      ConfigurationReconciler configurationReconciler,
      ConfigurationMapper configurationMapper) {
    this.configurationService = configurationService;
    this.configurationReconciler = configurationReconciler;
    this.configurationMapper = configurationMapper;
  }

  /**
   * Lists all configurations.
   *
   * @return the configurations
   */
  @Transactional(readOnly = true)
  public List<Configuration> configurations() {
    return configurationService.list().stream()
        .map(configurationMapper::map)
        .collect(Collectors.toList());
  }

  /**
   * Locates a configuration by id.
   *
   * @param id the configuration id
   * @return the configuration, or null when absent
   */
  @Transactional(readOnly = true)
  public Configuration configuration(String id) {
    return configurationService.locate(UUID.fromString(id))
        .map(configurationMapper::map)
        .orElse(null);
  }

  /**
   * Creates a configuration.
   *
   * @param input the configuration input
   * @return the saved configuration
   */
  @Transactional
  public Configuration createConfiguration(ConfigurationInput input) {
    return configurationMapper.map(configurationService.create(
        input.getName(), input.getDescription(),
        input.getEnabled() == null || input.getEnabled(), GaiaGraphQLSupport.asMap(input.getContent())));
  }

  /**
   * Updates a configuration.
   *
   * @param id the configuration id
   * @param input the configuration input
   * @return the saved configuration
   */
  @Transactional
  public Configuration updateConfiguration(String id, ConfigurationInput input) {
    return configurationMapper.map(configurationService.update(
        UUID.fromString(id), input.getName(), input.getDescription(),
        input.getEnabled() == null || input.getEnabled(), GaiaGraphQLSupport.asMap(input.getContent())));
  }

  /**
   * Deletes a configuration.
   *
   * @param id the configuration id
   * @return a success result
   */
  @Transactional
  public QueryResult deleteConfiguration(String id) {
    configurationService.deleteById(UUID.fromString(id));
    return QuerySuccess.newBuilder()
        .message("Configuration deleted")
        .id(id)
        .build();
  }

  /**
   * Applies a configuration's desired state immediately.
   *
   * @param id the configuration id
   * @return the reconciled configuration
   */
  @Transactional
  public Configuration applyConfiguration(String id) {
    return configurationMapper.map(configurationReconciler.reconcileById(UUID.fromString(id)));
  }
}

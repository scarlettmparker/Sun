package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.ApplyConfigurationResponse;
import com.sun.gaia.codegen.types.Configuration;
import com.sun.gaia.codegen.types.ConfigurationInput;
import com.sun.gaia.codegen.types.CreateConfigurationResponse;
import com.sun.gaia.codegen.types.DeleteConfigurationResponse;
import com.sun.gaia.codegen.types.UpdateConfigurationResponse;
import com.sun.gaia.graphql.mappers.ConfigurationMapper;
import com.sun.gaia.graphql.services.support.GaiaGraphQLSupport;
import com.sun.gaia.service.ConfigurationReconciler;
import com.sun.gaia.service.ConfigurationService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for configuration operations.
 */
@DgsComponent
public class ConfigurationDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationDataFetcher.class);

  private final ConfigurationService configurationService;
  private final ConfigurationReconciler configurationReconciler;
  private final ConfigurationMapper configurationMapper;

  public ConfigurationDataFetcher(
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
  @DgsData(parentType = "GaiaQueries", field = "configurations")
  @PreAuthorize("@permissions.has('graphql.gaia.configurations')")
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
  @DgsData(parentType = "GaiaQueries", field = "configuration")
  @PreAuthorize("@permissions.has('graphql.gaia.configuration')")
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
  @DgsData(parentType = "GaiaMutations", field = "createConfiguration")
  @PreAuthorize("@permissions.has('graphql.gaia.createConfiguration')")
  @Transactional
  public CreateConfigurationResponse createConfiguration(ConfigurationInput input) {
    Configuration configuration = configurationMapper.map(configurationService.create(
        input.getName(), input.getDescription(),
        input.getEnabled() == null || input.getEnabled(), GaiaGraphQLSupport.asMap(input.getContent())));
    return CreateConfigurationResponse.newBuilder()
        .message("Configuration created")
        .configuration(configuration)
        .build();
  }

  /**
   * Updates a configuration.
   *
   * @param id the configuration id
   * @param input the configuration input
   * @return the saved configuration
   */
  @DgsData(parentType = "GaiaMutations", field = "updateConfiguration")
  @PreAuthorize("@permissions.has('graphql.gaia.updateConfiguration')")
  @Transactional
  public UpdateConfigurationResponse updateConfiguration(String id, ConfigurationInput input) {
    Configuration configuration = configurationMapper.map(configurationService.update(
        UUID.fromString(id), input.getName(), input.getDescription(),
        input.getEnabled() == null || input.getEnabled(), GaiaGraphQLSupport.asMap(input.getContent())));
    return UpdateConfigurationResponse.newBuilder()
        .message("Configuration updated")
        .configuration(configuration)
        .build();
  }

  /**
   * Deletes a configuration.
   *
   * @param id the configuration id
   * @return a success result
   */
  @DgsData(parentType = "GaiaMutations", field = "deleteConfiguration")
  @PreAuthorize("@permissions.has('graphql.gaia.deleteConfiguration')")
  @Transactional
  public DeleteConfigurationResponse deleteConfiguration(String id) {
    configurationService.deleteById(UUID.fromString(id));
    return DeleteConfigurationResponse.newBuilder()
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
  @DgsData(parentType = "GaiaMutations", field = "applyConfiguration")
  @PreAuthorize("@permissions.has('graphql.gaia.applyConfiguration')")
  @Transactional
  public ApplyConfigurationResponse applyConfiguration(String id) {
    Configuration configuration = configurationMapper.map(
        configurationReconciler.reconcileById(UUID.fromString(id)));
    return ApplyConfigurationResponse.newBuilder()
        .message("Configuration applied")
        .configuration(configuration)
        .build();
  }
}

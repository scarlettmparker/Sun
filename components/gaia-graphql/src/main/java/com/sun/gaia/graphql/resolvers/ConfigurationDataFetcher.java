package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.Configuration;
import com.sun.gaia.codegen.types.ConfigurationInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.graphql.services.ConfigurationGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for configuration operations.
 */
@DgsComponent
public class ConfigurationDataFetcher {

  private final ConfigurationGraphQLService configurationGraphQLService;

  public ConfigurationDataFetcher(ConfigurationGraphQLService configurationGraphQLService) {
    this.configurationGraphQLService = configurationGraphQLService;
  }

  /**
   * Lists all configurations.
   *
   * @return the configurations
   */
  @DgsData(parentType = "GaiaQueries", field = "configurations")
  @PreAuthorize("@permissions.has('graphql.gaia.configurations')")
  public List<Configuration> configurations() {
    return configurationGraphQLService.configurations();
  }

  /**
   * Locates a configuration by id.
   *
   * @param id the configuration id
   * @return the configuration, or null when absent
   */
  @DgsData(parentType = "GaiaQueries", field = "configuration")
  @PreAuthorize("@permissions.has('graphql.gaia.configuration')")
  public Configuration configuration(String id) {
    return configurationGraphQLService.configuration(id);
  }

  /**
   * Creates a configuration.
   *
   * @param input the configuration input
   * @return the saved configuration
   */
  @DgsData(parentType = "GaiaMutations", field = "createConfiguration")
  @PreAuthorize("@permissions.has('graphql.gaia.createConfiguration')")
  public Configuration createConfiguration(ConfigurationInput input) {
    return configurationGraphQLService.createConfiguration(input);
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
  public Configuration updateConfiguration(String id, ConfigurationInput input) {
    return configurationGraphQLService.updateConfiguration(id, input);
  }

  /**
   * Deletes a configuration.
   *
   * @param id the configuration id
   * @return a success result
   */
  @DgsData(parentType = "GaiaMutations", field = "deleteConfiguration")
  @PreAuthorize("@permissions.has('graphql.gaia.deleteConfiguration')")
  public QueryResult deleteConfiguration(String id) {
    return configurationGraphQLService.deleteConfiguration(id);
  }

  /**
   * Applies a configuration's desired state immediately.
   *
   * @param id the configuration id
   * @return the reconciled configuration
   */
  @DgsData(parentType = "GaiaMutations", field = "applyConfiguration")
  @PreAuthorize("@permissions.has('graphql.gaia.applyConfiguration')")
  public Configuration applyConfiguration(String id) {
    return configurationGraphQLService.applyConfiguration(id);
  }
}

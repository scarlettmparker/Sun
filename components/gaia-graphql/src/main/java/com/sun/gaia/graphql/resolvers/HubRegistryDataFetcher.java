package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.HubRegistry;
import com.sun.gaia.codegen.types.HubRegistryInput;
import com.sun.gaia.graphql.services.HubRegistryGraphQLService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for hub registry operations.
 */
@DgsComponent
public class HubRegistryDataFetcher {

  private final HubRegistryGraphQLService hubRegistryGraphQLService;

  public HubRegistryDataFetcher(HubRegistryGraphQLService hubRegistryGraphQLService) {
    this.hubRegistryGraphQLService = hubRegistryGraphQLService;
  }

  /**
   * Returns the hub registry.
   *
   * @return the hub registry
   */
  @DgsData(parentType = "GaiaQueries", field = "hubRegistry")
  @PreAuthorize("permitAll()")
  public HubRegistry hubRegistry() {
    return hubRegistryGraphQLService.hubRegistry();
  }

  /**
   * Validates and persists the hub registry.
   *
   * @param input the hub registry input
   * @return the saved hub registry
   */
  @DgsData(parentType = "GaiaMutations", field = "saveRegistry")
  @PreAuthorize("@permissions.has('graphql.gaia.hubRegistry')")
  public HubRegistry saveRegistry(HubRegistryInput input) {
    return hubRegistryGraphQLService.saveRegistry(input);
  }
}

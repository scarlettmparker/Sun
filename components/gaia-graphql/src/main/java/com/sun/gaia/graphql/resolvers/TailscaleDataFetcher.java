package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.TailscaleDevice;
import com.sun.gaia.graphql.services.TailscaleGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for Tailscale device operations.
 */
@DgsComponent
public class TailscaleDataFetcher {

  private final TailscaleGraphQLService tailscaleGraphQLService;

  public TailscaleDataFetcher(TailscaleGraphQLService tailscaleGraphQLService) {
    this.tailscaleGraphQLService = tailscaleGraphQLService;
  }

  @DgsData(parentType = "GaiaQueries", field = "tailscaleDevices")
  @PreAuthorize("@permissions.has('graphql.gaia.tailscaleDevices')")
  public List<TailscaleDevice> tailscaleDevices() {
    return tailscaleGraphQLService.tailscaleDevices();
  }

  @DgsData(parentType = "GaiaQueries", field = "tailscaleDevice")
  @PreAuthorize("@permissions.has('graphql.gaia.tailscaleDevice')")
  public TailscaleDevice tailscaleDevice(String id) {
    return tailscaleGraphQLService.tailscaleDevice(id);
  }

  @DgsData(parentType = "GaiaMutations", field = "expireTailscaleDevice")
  @PreAuthorize("@permissions.has('graphql.gaia.expireTailscaleDevice')")
  public QueryResult expireTailscaleDevice(String id) {
    return tailscaleGraphQLService.expireTailscaleDevice(id);
  }
}

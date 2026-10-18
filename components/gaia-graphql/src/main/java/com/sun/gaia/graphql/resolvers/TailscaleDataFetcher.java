package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.audit.annotation.Audited;
import com.sun.gaia.codegen.types.ExpireTailscaleDeviceResponse;
import com.sun.gaia.codegen.types.TailscaleDevice;
import com.sun.gaia.graphql.mappers.TailscaleDeviceMapper;
import com.sun.gaia.service.TailscaleDeviceService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for Tailscale device operations.
 */
@DgsComponent
public class TailscaleDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(TailscaleDataFetcher.class);

  private final TailscaleDeviceService tailscaleDeviceService;
  private final TailscaleDeviceMapper tailscaleDeviceMapper;

  public TailscaleDataFetcher(
      TailscaleDeviceService tailscaleDeviceService,
      TailscaleDeviceMapper tailscaleDeviceMapper) {
    this.tailscaleDeviceService = tailscaleDeviceService;
    this.tailscaleDeviceMapper = tailscaleDeviceMapper;
  }

  @Audited(event = "TAILSCALEDEVICE_LIST", entity = "TailscaleDevice")
  @DgsData(parentType = "GaiaQueries", field = "tailscaleDevices")
  @PreAuthorize("@permissions.has('graphql.gaia.tailscaleDevices')")
  @Transactional(readOnly = true)
  public List<TailscaleDevice> tailscaleDevices() {
    return tailscaleDeviceMapper.map(tailscaleDeviceService.listAll());
  }

  @Audited(event = "TAILSCALEDEVICE_VIEW", entity = "TailscaleDevice")
  @DgsData(parentType = "GaiaQueries", field = "tailscaleDevice")
  @PreAuthorize("@permissions.has('graphql.gaia.tailscaleDevice')")
  @Transactional(readOnly = true)
  public TailscaleDevice tailscaleDevice(String id) {
    try {
      return tailscaleDeviceMapper.map(tailscaleDeviceService.findById(UUID.fromString(id)));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Audited(event = "TAILSCALEDEVICE_EXPIRE", entity = "TailscaleDevice")
  @DgsData(parentType = "GaiaMutations", field = "expireTailscaleDevice")
  @PreAuthorize("@permissions.has('graphql.gaia.expireTailscaleDevice')")
  @Transactional
  public ExpireTailscaleDeviceResponse expireTailscaleDevice(String id) {
    return ExpireTailscaleDeviceResponse.newBuilder()
        .message("Tailscale device expired")
        .device(tailscaleDeviceMapper.map(tailscaleDeviceService.markExpired(UUID.fromString(id))))
        .build();
  }
}

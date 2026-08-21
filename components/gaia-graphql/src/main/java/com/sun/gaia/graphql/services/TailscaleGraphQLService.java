package com.sun.gaia.graphql.services;

import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.TailscaleDevice;
import com.sun.gaia.graphql.mappers.TailscaleDeviceMapper;
import com.sun.gaia.service.TailscaleDeviceService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for Tailscale devices.
 */
@Service
public class TailscaleGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(TailscaleGraphQLService.class);

  private final TailscaleDeviceService tailscaleDeviceService;
  private final TailscaleDeviceMapper tailscaleDeviceMapper;

  public TailscaleGraphQLService(
      TailscaleDeviceService tailscaleDeviceService,
      TailscaleDeviceMapper tailscaleDeviceMapper) {
    this.tailscaleDeviceService = tailscaleDeviceService;
    this.tailscaleDeviceMapper = tailscaleDeviceMapper;
  }

  /**
   * Returns all tracked Tailscale devices.
   */
  @Transactional(readOnly = true)
  public List<TailscaleDevice> tailscaleDevices() {
    return tailscaleDeviceMapper.map(tailscaleDeviceService.listAll());
  }

  /**
   * Returns a single Tailscale device by id.
   *
   * @param id the Gaia device record id.
   * @return the device, or null if not found.
   */
  @Transactional(readOnly = true)
  public TailscaleDevice tailscaleDevice(String id) {
    try {
      return tailscaleDeviceMapper.map(tailscaleDeviceService.findById(UUID.fromString(id)));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Marks a Tailscale device as expired.
   *
   * @param id the Gaia device record id.
   * @return a success result.
   */
  @Transactional
  public QueryResult expireTailscaleDevice(String id) {
    tailscaleDeviceService.markExpired(UUID.fromString(id));
    return QuerySuccess.newBuilder()
        .message("Tailscale device expired")
        .id(id)
        .build();
  }
}

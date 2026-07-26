package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.TailscaleDeviceEntity;
import com.sun.gaia.model.enums.DeviceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Data access for Tailscale devices tracked in Gaia.
 */
public interface TailscaleDeviceRepository
    extends BaseRepository<TailscaleDeviceEntity>, JpaSpecificationExecutor<TailscaleDeviceEntity> {

    /**
     * Finds a device by its Headscale node id.
     */
    Optional<TailscaleDeviceEntity> findByHeadscaleId(long headscaleId);

    /**
     * Returns all devices with the given status.
     */
    List<TailscaleDeviceEntity> findByStatus(DeviceStatus status);
}

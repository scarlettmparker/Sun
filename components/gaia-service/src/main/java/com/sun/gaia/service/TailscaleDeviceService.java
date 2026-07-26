package com.sun.gaia.service;

import com.sun.gaia.model.TailscaleDeviceEntity;
import com.sun.gaia.model.enums.DeviceStatus;
import com.sun.gaia.repository.TailscaleDeviceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages Tailscale device records in Gaia. Devices are synced from
 * Headscale by the WhitelistReconciler and can be expired or suspended
 * independently.
 */
@Service
public class TailscaleDeviceService {

    private static final Logger log = LoggerFactory.getLogger(TailscaleDeviceService.class);

    private final TailscaleDeviceRepository repository;

    public TailscaleDeviceService(TailscaleDeviceRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns all tracked Tailscale devices.
     */
    @Transactional(readOnly = true)
    public List<TailscaleDeviceEntity> listAll() {
        return repository.findAll();
    }

    /**
     * Returns a single device by id.
     */
    @Transactional(readOnly = true)
    public TailscaleDeviceEntity findById(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tailscale device not found: " + id));
    }

    /**
     * Creates or updates a device record from Headscale node data.
     */
    @Transactional
    public TailscaleDeviceEntity upsertFromHeadscale(long headscaleId, String name, String ipv4, String lastSeen, boolean online) {
        var existing = repository.findByHeadscaleId(headscaleId);
        if (existing.isPresent()) {
            TailscaleDeviceEntity device = existing.get();
            device.setName(name);
            device.setIpv4(ipv4);
            device.setLastSeen(lastSeen);
            device.setOnline(online);
            return repository.save(device);
        }
        TailscaleDeviceEntity device = new TailscaleDeviceEntity();
        device.setHeadscaleId(headscaleId);
        device.setName(name);
        device.setIpv4(ipv4);
        device.setLastSeen(lastSeen);
        device.setOnline(online);
        device.setStatus(DeviceStatus.ACTIVE);
        return repository.save(device);
    }

    /**
     * Marks a device as expired and records the expiration time.
     */
    @Transactional
    public TailscaleDeviceEntity markExpired(UUID id) {
        TailscaleDeviceEntity device = findById(id);
        device.setStatus(DeviceStatus.EXPIRED);
        device.setExpiredAt(LocalDateTime.now());
        return repository.save(device);
    }

    /**
     * Deletes a device record.
     */
    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}

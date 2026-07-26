package com.sun.gaia.model;

import com.sun.base.model.BaseEntity;
import com.sun.gaia.model.enums.DeviceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * A Tailscale device tracked in Gaia. Synced from Headscale via the
 * WhitelistReconciler and used to track expiration and suspension.
 */
@Entity
@Table(name = "gaia_tailscale_devices")
public class TailscaleDeviceEntity extends BaseEntity {

    @Column(nullable = false)
    private long headscaleId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 45)
    private String ipv4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(columnDefinition = "timestamp(6)")
    private LocalDateTime expiredAt;

    @Column(length = 255)
    private String lastSeen;

    @Column(nullable = false)
    private boolean online = false;

    public long getHeadscaleId() {
        return headscaleId;
    }

    public void setHeadscaleId(long headscaleId) {
        this.headscaleId = headscaleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpv4() {
        return ipv4;
    }

    public void setIpv4(String ipv4) {
        this.ipv4 = ipv4;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}

package com.sun.gaia.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One entry in the IP whitelist.
 */
@Entity
@Table(name = "gaia_ip_whitelist_entries")
public class IpWhitelistEntryEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String pattern;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean immutable = false;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }
}

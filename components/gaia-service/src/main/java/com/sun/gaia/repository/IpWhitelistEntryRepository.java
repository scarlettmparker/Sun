package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.IpWhitelistEntryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Data access for IP whitelist entries.
 */
public interface IpWhitelistEntryRepository
    extends BaseRepository<IpWhitelistEntryEntity>, JpaSpecificationExecutor<IpWhitelistEntryEntity> {

    /**
     * Returns only the entries that are currently enabled.
     */
    List<IpWhitelistEntryEntity> findByEnabledTrue();
}

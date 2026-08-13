package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.ApiKeyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Data access for API keys.
 */
public interface ApiKeyRepository
    extends BaseRepository<ApiKeyEntity>, JpaSpecificationExecutor<ApiKeyEntity> {

  /**
   * Finds a key by its SHA-256 hash.
   *
   * @param keyHash the hashed key value
   * @return the matching key, if any
   */
  Optional<ApiKeyEntity> findByKeyHash(String keyHash);
}

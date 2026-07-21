package com.sun.dionysus.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.dionysus.model.MagnetDetailEntity;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Repository for MagnetDetail entities.
 */
@Repository
public interface MagnetDetailEntityRepository extends BaseRepository<MagnetDetailEntity> {

  Optional<MagnetDetailEntity> findByInfoHash(String infoHash);

  boolean existsByInfoHash(String infoHash);
}

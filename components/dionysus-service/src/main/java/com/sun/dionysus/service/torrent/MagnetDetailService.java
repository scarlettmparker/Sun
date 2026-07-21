package com.sun.dionysus.service.torrent;

import com.sun.base.service.BaseService;
import com.sun.dionysus.model.MagnetDetailEntity;
import com.sun.dionysus.repository.MagnetDetailEntityRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for MagnetDetail entities, the torrent metadata captured at add time.
 */
@Service
@Transactional
public class MagnetDetailService extends BaseService<MagnetDetailEntity> {

  private final MagnetDetailEntityRepository magnetRepository;

  public MagnetDetailService(MagnetDetailEntityRepository repository) {
    super(repository);
    this.magnetRepository = repository;
  }

  /**
   * Finds an existing magnet record by info hash, so a re-add reuses metadata.
   */
  public Optional<MagnetDetailEntity> findByInfoHash(String infoHash) {
    return magnetRepository.findByInfoHash(infoHash);
  }
}

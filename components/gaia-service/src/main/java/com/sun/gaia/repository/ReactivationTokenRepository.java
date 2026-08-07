package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.ReactivationTokenEntity;
import java.util.Optional;

public interface ReactivationTokenRepository extends BaseRepository<ReactivationTokenEntity> {

  Optional<ReactivationTokenEntity> findByToken(String token);
}

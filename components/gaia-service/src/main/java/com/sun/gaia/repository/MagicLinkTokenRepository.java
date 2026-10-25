package com.sun.gaia.repository;

import com.sun.gaia.model.MagicLinkTokenEntity;
import com.sun.base.repository.BaseRepository;
import java.util.Optional;
import java.util.UUID;

public interface MagicLinkTokenRepository extends BaseRepository<MagicLinkTokenEntity> {
  Optional<MagicLinkTokenEntity> findByToken(String token);

  void deleteByAccountId(UUID accountId);
}
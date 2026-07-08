package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderAccountEntity;
import java.util.Optional;
import java.util.UUID;

public interface ReaderAccountRepository extends BaseRepository<ReaderAccountEntity> {

  Optional<ReaderAccountEntity> findByDiscordId(String discordId);

  Optional<ReaderAccountEntity> findByGaiaAccountId(UUID gaiaAccountId);
}

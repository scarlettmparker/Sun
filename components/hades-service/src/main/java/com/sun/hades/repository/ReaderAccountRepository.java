package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderAccountEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReaderAccountRepository extends BaseRepository<ReaderAccountEntity> {

  Optional<ReaderAccountEntity> findByDiscordId(String discordId);

  List<ReaderAccountEntity> findByDiscordIdIn(Collection<String> discordIds);

  Optional<ReaderAccountEntity> findByGaiaAccountId(UUID gaiaAccountId);
}

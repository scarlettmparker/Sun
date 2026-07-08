package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.AccountEntity;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends BaseRepository<AccountEntity> {

  Optional<AccountEntity> findByUsername(String username);

  Optional<AccountEntity> findByPersonId(UUID personId);

  Optional<AccountEntity> findByProviderAndProviderId(String provider, String providerId);
}

package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends BaseRepository<AccountEntity> {

  Optional<AccountEntity> findByUsername(String username);

  Optional<AccountEntity> findByUsernameAndAccountType(String username, AccountType accountType);

  List<AccountEntity> findByAccountType(AccountType accountType);

  Optional<AccountEntity> findByPersonId(UUID personId);

  Optional<AccountEntity> findByProviderAndProviderId(String provider, String providerId);
}

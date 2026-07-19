package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends BaseRepository<AccountEntity> {

  Optional<AccountEntity> findByUsername(String username);

  Optional<AccountEntity> findByUsernameAndAccountType(String username, AccountType accountType);

  List<AccountEntity> findByAccountType(AccountType accountType);

  Optional<AccountEntity> findByPersonId(UUID personId);

  Optional<AccountEntity> findByProviderAndProviderId(String provider, String providerId);

  /**
   * Permission patterns granted to the caller's person (any of that person's
   * accounts), directly or via roles.
   */
  @Query(
      value =
          "SELECT permission FROM gaia_account_permissions "
              + "WHERE account_id IN (SELECT id FROM gaia_accounts WHERE person_id = "
              + "(SELECT person_id FROM gaia_accounts WHERE id = :accountId)) "
              + "UNION "
              + "SELECT rp.permission FROM gaia_role_permissions rp "
              + "JOIN gaia_account_roles ar ON ar.role_id = rp.role_id "
              + "WHERE ar.account_id IN (SELECT id FROM gaia_accounts WHERE person_id = "
              + "(SELECT person_id FROM gaia_accounts WHERE id = :accountId))",
      nativeQuery = true)
  List<String> findEffectivePermissions(@Param("accountId") UUID accountId);
}


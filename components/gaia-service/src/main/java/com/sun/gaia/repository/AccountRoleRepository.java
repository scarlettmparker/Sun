package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.AccountRoleEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRoleRepository extends BaseRepository<AccountRoleEntity> {

  List<AccountRoleEntity> findByAccountId(UUID accountId);

  List<AccountRoleEntity> findByRoleId(UUID roleId);

  void deleteByAccountId(UUID accountId);

  void deleteByRoleId(UUID roleId);

  /**
   * Resolves role names for several accounts in one query.
   *
   * @param accountIds accounts to resolve
   * @return one row per account/role pair
   */
  @Query(
      "select ar.accountId as accountId, r.name as roleName "
          + "from AccountRoleEntity ar, RoleEntity r "
          + "where r.id = ar.roleId and ar.accountId in :accountIds")
  List<AccountRoleName> findRoleNamesByAccountIds(
      @Param("accountIds") Collection<UUID> accountIds);

  /**
   * Flat account id to role name row.
   */
  interface AccountRoleName {
    /**
     * Account the role belongs to.
     *
     * @return account id
     */
    UUID getAccountId();

    /**
     * Granted role name.
     *
     * @return role name
     */
    String getRoleName();
  }
}

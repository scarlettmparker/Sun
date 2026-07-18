package com.sun.gaia.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * Join between an account and a role.
 */
@Entity
@Table(
    name = "gaia_account_roles",
    uniqueConstraints =
        @UniqueConstraint(
            name = "gaia_account_roles_account_id_role_id_key",
            columnNames = {"account_id", "role_id"}))
public class AccountRoleEntity extends BaseEntity {

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public UUID getRoleId() {
    return roleId;
  }

  public void setRoleId(UUID roleId) {
    this.roleId = roleId;
  }
}

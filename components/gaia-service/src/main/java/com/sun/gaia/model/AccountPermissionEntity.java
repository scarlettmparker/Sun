package com.sun.gaia.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * A permission string (possibly a wildcard pattern) granted directly to an account.
 */
@Entity
@Table(
    name = "gaia_account_permissions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "gaia_account_permissions_account_id_permission_key",
            columnNames = {"account_id", "permission"}))
public class AccountPermissionEntity extends BaseEntity {

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "permission", nullable = false)
  private String permission;

  public UUID getAccountId() {
    return accountId;
  }

  public void setAccountId(UUID accountId) {
    this.accountId = accountId;
  }

  public String getPermission() {
    return permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }
}

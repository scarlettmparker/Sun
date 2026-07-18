package com.sun.gaia.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * A permission string (possibly a wildcard pattern) granted by a role.
 */
@Entity
@Table(
    name = "gaia_role_permissions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "gaia_role_permissions_role_id_permission_key",
            columnNames = {"role_id", "permission"}))
public class RolePermissionEntity extends BaseEntity {

  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  @Column(name = "permission", nullable = false)
  private String permission;

  public UUID getRoleId() {
    return roleId;
  }

  public void setRoleId(UUID roleId) {
    this.roleId = roleId;
  }

  public String getPermission() {
    return permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }
}

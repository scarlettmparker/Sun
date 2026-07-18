package com.sun.gaia.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A named bundle of permission strings granted to accounts.
 */
@Entity
@Table(
    name = "gaia_roles",
    uniqueConstraints = @UniqueConstraint(name = "gaia_roles_name_key", columnNames = "name"))
public class RoleEntity extends BaseEntity {

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "description")
  private String description;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}

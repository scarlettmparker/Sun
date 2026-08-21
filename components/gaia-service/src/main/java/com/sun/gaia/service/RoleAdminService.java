package com.sun.gaia.service;

import com.sun.gaia.model.AccountPermissionEntity;
import com.sun.gaia.model.AccountRoleEntity;
import com.sun.gaia.model.RoleEntity;
import com.sun.gaia.model.RolePermissionEntity;
import com.sun.gaia.repository.AccountPermissionRepository;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.repository.AccountRoleRepository;
import com.sun.gaia.repository.RolePermissionRepository;
import com.sun.gaia.repository.RoleRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages account-scoped role and permission assignments with replace semantics.
 */
@Service
public class RoleAdminService {

  private final AccountRepository accountRepository;
  private final RoleRepository roleRepository;
  private final AccountRoleRepository accountRoleRepository;
  private final AccountPermissionRepository accountPermissionRepository;
  private final RolePermissionRepository rolePermissionRepository;

  public RoleAdminService(
      AccountRepository accountRepository,
      RoleRepository roleRepository,
      AccountRoleRepository accountRoleRepository,
      AccountPermissionRepository accountPermissionRepository,
      RolePermissionRepository rolePermissionRepository) {
    this.accountRepository = accountRepository;
    this.roleRepository = roleRepository;
    this.accountRoleRepository = accountRoleRepository;
    this.accountPermissionRepository = accountPermissionRepository;
    this.rolePermissionRepository = rolePermissionRepository;
  }

  /**
   * Lists all roles ordered by name.
   *
   * @return the roles
   */
  @Transactional(readOnly = true)
  public List<RoleEntity> listRoles() {
    return roleRepository.findAllByOrderByNameAsc();
  }

  /**
   * Locates a role by id.
   *
   * @param roleId the role id
   * @return the role, or empty
   */
  @Transactional(readOnly = true)
  public java.util.Optional<RoleEntity> findRoleById(UUID roleId) {
    return roleRepository.findById(roleId);
  }

  /**
   * Creates a new role.
   *
   * @param name the role name
   * @param description the optional description
   * @return the created role
   */
  @Transactional
  public RoleEntity createRole(String name, String description) {
    String trimmed = requireName(name);
    if (roleRepository.findByName(trimmed).isPresent()) {
      throw new IllegalArgumentException("Role already exists: " + trimmed);
    }
    RoleEntity entity = new RoleEntity();
    entity.setName(trimmed);
    entity.setDescription(description);
    return roleRepository.save(entity);
  }

  /**
   * Deletes a role and its assignments.
   *
   * @param roleId the role id
   */
  @Transactional
  public void deleteRole(UUID roleId) {
    RoleEntity role = roleRepository.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
    rolePermissionRepository.deleteByRoleId(role.getId());
    accountRoleRepository.deleteByRoleId(role.getId());
    roleRepository.delete(role);
  }

  /**
   * Returns direct role names for the account (account-scoped, not person).
   *
   * @param accountId the account id
   * @return the role names
   */
  @Transactional(readOnly = true)
  public List<String> accountRoleNames(UUID accountId) {
    requireAccount(accountId);
    List<AccountRoleEntity> links = accountRoleRepository.findByAccountId(accountId);
    List<String> names = new ArrayList<>();
    for (AccountRoleEntity link : links) {
      roleRepository.findById(link.getRoleId()).ifPresent(r -> names.add(r.getName()));
    }
    names.sort(String::compareTo);
    return names;
  }

  /**
   * Returns direct permission strings for the account (account-scoped).
   *
   * @param accountId the account id
   * @return the permission strings
   */
  @Transactional(readOnly = true)
  public List<String> accountPermissions(UUID accountId) {
    requireAccount(accountId);
    List<String> perms = new ArrayList<>();
    for (AccountPermissionEntity e : accountPermissionRepository.findByAccountId(accountId)) {
      perms.add(e.getPermission());
    }
    perms.sort(String::compareTo);
    return perms;
  }

  /**
   * Returns permission strings for the role.
   *
   * @param roleId the role id
   * @return the permission strings
   */
  @Transactional(readOnly = true)
  public List<String> rolePermissions(UUID roleId) {
    RoleEntity role = roleRepository.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
    List<String> perms = new ArrayList<>();
    for (RolePermissionEntity e : rolePermissionRepository.findByRoleId(role.getId())) {
      perms.add(e.getPermission());
    }
    perms.sort(String::compareTo);
    return perms;
  }

  /**
   * Replaces the account's roles with the given set.
   *
   * @param accountId the account id
   * @param roleNames the desired role names (empty = remove all)
   * @return the resulting role names
   */
  @Transactional
  public List<String> setAccountRoles(UUID accountId, List<String> roleNames) {
    requireAccount(accountId);
    Set<String> desired = normaliseNames(roleNames);
    for (String name : desired) {
      if (roleRepository.findByName(name).isEmpty()) {
        throw new IllegalArgumentException("Unknown role: " + name);
      }
    }
    accountRoleRepository.deleteByAccountId(accountId);
    accountRoleRepository.flush();
    for (String name : desired) {
      RoleEntity role = roleRepository.findByName(name).orElseThrow();
      AccountRoleEntity link = new AccountRoleEntity();
      link.setAccountId(accountId);
      link.setRoleId(role.getId());
      accountRoleRepository.save(link);
    }
    return accountRoleNames(accountId);
  }

  /**
   * Replaces the account's direct permissions with the given set.
   *
   * @param accountId the account id
   * @param permissions the desired permission strings (empty = remove all)
   * @return the resulting permissions
   */
  @Transactional
  public List<String> setAccountPermissions(UUID accountId, List<String> permissions) {
    requireAccount(accountId);
    Set<String> desired = normalisePermissions(permissions);
    accountPermissionRepository.deleteByAccountId(accountId);
    accountPermissionRepository.flush();
    for (String perm : desired) {
      AccountPermissionEntity entity = new AccountPermissionEntity();
      entity.setAccountId(accountId);
      entity.setPermission(perm);
      accountPermissionRepository.save(entity);
    }
    return accountPermissions(accountId);
  }

  /**
   * Replaces a role's permissions with the given set.
   *
   * @param roleId the role id
   * @param permissions the desired permission strings (empty = remove all)
   * @return the resulting permissions
   */
  @Transactional
  public List<String> setRolePermissions(UUID roleId, List<String> permissions) {
    RoleEntity role = roleRepository.findById(roleId)
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
    Set<String> desired = normalisePermissions(permissions);
    rolePermissionRepository.deleteByRoleId(role.getId());
    rolePermissionRepository.flush();
    for (String perm : desired) {
      RolePermissionEntity entity = new RolePermissionEntity();
      entity.setRoleId(role.getId());
      entity.setPermission(perm);
      rolePermissionRepository.save(entity);
    }
    return rolePermissions(role.getId());
  }

  /**
   * Lists all distinct permission strings known to the system.
   *
   * @return the distinct permissions sorted
   */
  @Transactional(readOnly = true)
  public List<String> allPermissions() {
    Set<String> all = new LinkedHashSet<>();
    all.addAll(accountPermissionRepository.findDistinctPermissions());
    all.addAll(rolePermissionRepository.findDistinctPermissions());
    List<String> sorted = new ArrayList<>(all);
    sorted.sort(String::compareTo);
    return sorted;
  }

  private void requireAccount(UUID accountId) {
    accountRepository.findById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
  }

  private String requireName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Role name is required");
    }
    return name.trim();
  }

  private Set<String> normaliseNames(List<String> values) {
    if (values == null) {
      return Set.of();
    }
    Set<String> result = new LinkedHashSet<>();
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        result.add(v.trim());
      }
    }
    return result;
  }

  private Set<String> normalisePermissions(List<String> values) {
    if (values == null) {
      return Set.of();
    }
    Set<String> result = new LinkedHashSet<>();
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        result.add(v.trim());
      }
    }
    return result;
  }
}

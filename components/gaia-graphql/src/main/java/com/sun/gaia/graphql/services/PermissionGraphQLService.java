package com.sun.gaia.graphql.services;

import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.RoleAdminService;
import com.sun.gaia.service.UserContextHolder;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for permissions and role assignments.
 */
@Service
public class PermissionGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(PermissionGraphQLService.class);

  private final AccountRepository accountRepository;
  private final RoleAdminService roleAdminService;

  public PermissionGraphQLService(
      AccountRepository accountRepository,
      RoleAdminService roleAdminService) {
    this.accountRepository = accountRepository;
    this.roleAdminService = roleAdminService;
  }

  /**
   * Returns the caller's role key strings.
   */
  @Transactional(readOnly = true)
  public List<String> myRoles() {
    UUID userId = UserContextHolder.getUserId();
    if (userId == null) return List.of();
    return accountRepository.findEffectiveRoleNames(userId);
  }

  /**
   * Returns an active remote account's effective permission patterns.
   *
   * @param remoteUserType the remote identity type
   * @param remoteUserId the remote identity id
   * @return the permission patterns, or empty when the account does not exist
   */
  @Transactional(readOnly = true)
  public List<String> effectivePermissions(RemoteUserType remoteUserType, String remoteUserId) {
    if (remoteUserType != RemoteUserType.DISCORD || remoteUserId == null || remoteUserId.isBlank()) {
      return List.of();
    }
    return accountRepository
        .findByProviderStatus("discord", remoteUserId, AccountStatus.ACTIVE)
        .map(account -> accountRepository.findEffectivePermissions(account.getId()))
        .orElseGet(List::of);
  }

  /**
   * Returns direct permission strings for the account.
   *
   * @param accountId the account id
   * @return the permission strings
   */
  @Transactional(readOnly = true)
  public List<String> accountPermissions(String accountId) {
    return roleAdminService.accountPermissions(UUID.fromString(accountId));
  }

  /**
   * Returns permission strings for the role.
   *
   * @param roleId the role id
   * @return the permission strings
   */
  @Transactional(readOnly = true)
  public List<String> rolePermissions(String roleId) {
    return roleAdminService.rolePermissions(UUID.fromString(roleId));
  }

  /**
   * Lists all distinct permissions known to the system.
   *
   * @return the distinct permissions
   */
  @Transactional(readOnly = true)
  public List<String> allPermissions() {
    return roleAdminService.allPermissions();
  }

  /**
   * Replaces the account's direct permissions with the given set.
   *
   * @param accountId the account id
   * @param permissions the desired permissions
   * @return a success result
   */
  @Transactional
  public QueryResult setAccountPermissions(String accountId, List<String> permissions) {
    roleAdminService.setAccountPermissions(UUID.fromString(accountId), permissions);
    logger.info("Set permissions for account {}", accountId);
    return QuerySuccess.newBuilder().message("Account permissions updated").id(accountId).build();
  }

  /**
   * Replaces a role's permissions with the given set.
   *
   * @param roleId the role id
   * @param permissions the desired permissions
   * @return a success result
   */
  @Transactional
  public QueryResult setRolePermissions(String roleId, List<String> permissions) {
    roleAdminService.setRolePermissions(UUID.fromString(roleId), permissions);
    logger.info("Set permissions for role {}", roleId);
    return QuerySuccess.newBuilder().message("Role permissions updated").id(roleId).build();
  }
}

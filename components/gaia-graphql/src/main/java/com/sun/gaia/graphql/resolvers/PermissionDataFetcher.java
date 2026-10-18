package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.audit.annotation.Audited;
import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.codegen.types.SetAccountPermissionsResponse;
import com.sun.gaia.codegen.types.SetRolePermissionsResponse;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.RoleAdminService;
import com.sun.gaia.service.UserContextHolder;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for permission operations.
 */
@DgsComponent
public class PermissionDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(PermissionDataFetcher.class);

  private final AccountRepository accountRepository;
  private final RoleAdminService roleAdminService;

  public PermissionDataFetcher(
      AccountRepository accountRepository,
      RoleAdminService roleAdminService) {
    this.accountRepository = accountRepository;
    this.roleAdminService = roleAdminService;
  }

  /**
   * Returns the caller's role key strings, or empty when unauthenticated.
   */
  @Audited(event = "ROLE_LIST", entity = "Role")
  @DgsData(parentType = "GaiaQueries", field = "myRoles")
  @PreAuthorize("permitAll()")
  @Transactional(readOnly = true)
  public List<String> myRoles() {
    UUID userId = UserContextHolder.getUserId();
    if (userId == null) return List.of();
    return accountRepository.findEffectiveRoleNames(userId);
  }

   /**
    * Returns a remote account's effective permission patterns.
    */
  @Audited(event = "ACCOUNT_PERMISSIONS", entity = "Account")
  @DgsData(parentType = "GaiaQueries", field = "effectivePermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.effectivePermissions')")
  @Transactional(readOnly = true)
  public List<String> effectivePermissions(
      RemoteUserType remoteUserType, String remoteUserId) {
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
    */
  @Audited(event = "ACCOUNT_PERMISSIONS", entity = "Account")
  @DgsData(parentType = "GaiaQueries", field = "accountPermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.accountPermissions')")
  @Transactional(readOnly = true)
  public List<String> accountPermissions(String accountId) {
    return roleAdminService.accountPermissions(UUID.fromString(accountId));
  }

   /**
    * Returns permission strings for the role.
    */
  @Audited(event = "ROLE_PERMISSIONS", entity = "Role")
  @DgsData(parentType = "GaiaQueries", field = "rolePermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.rolePermissions')")
  @Transactional(readOnly = true)
  public List<String> rolePermissions(String roleId) {
    return roleAdminService.rolePermissions(UUID.fromString(roleId));
  }

  /**
   * Lists all distinct permissions.
   */
  @Audited(event = "PERMISSION_LIST")
  @DgsData(parentType = "GaiaQueries", field = "allPermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.allPermissions')")
  @Transactional(readOnly = true)
  public List<String> allPermissions() {
    return roleAdminService.allPermissions();
  }

   /**
    * Replaces the account's direct permissions with the given set.
    */
  @Audited(event = "ACCOUNT_SETPERMISSIONS", entity = "Account")
  @DgsData(parentType = "GaiaMutations", field = "setAccountPermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.setAccountPermissions')")
  @Transactional
  public SetAccountPermissionsResponse setAccountPermissions(String accountId, List<String> permissions) {
    roleAdminService.setAccountPermissions(UUID.fromString(accountId), permissions);
    logger.info("Set permissions for account {}", accountId);
    return SetAccountPermissionsResponse.newBuilder()
        .message("Account permissions updated")
        .accountId(accountId)
        .permissions(permissions)
        .build();
  }

   /**
    * Replaces a role's permissions with the given set.
    */
  @Audited(event = "ROLE_SETPERMISSIONS", entity = "Role")
  @DgsData(parentType = "GaiaMutations", field = "setRolePermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.setRolePermissions')")
  @Transactional
  public SetRolePermissionsResponse setRolePermissions(String roleId, List<String> permissions) {
    roleAdminService.setRolePermissions(UUID.fromString(roleId), permissions);
    logger.info("Set permissions for role {}", roleId);
    return SetRolePermissionsResponse.newBuilder()
        .message("Role permissions updated")
        .roleId(roleId)
        .permissions(permissions)
        .build();
  }
}

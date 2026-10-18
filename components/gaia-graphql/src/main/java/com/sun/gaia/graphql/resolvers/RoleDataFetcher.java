package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.CreateRoleResponse;
import com.sun.gaia.codegen.types.DeleteRoleResponse;
import com.sun.gaia.codegen.types.Role;
import com.sun.gaia.codegen.types.SetAccountRolesResponse;
import com.sun.gaia.graphql.mappers.RoleMapper;
import com.sun.gaia.service.RoleAdminService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for role operations.
 */
@DgsComponent
public class RoleDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(RoleDataFetcher.class);

  private final RoleAdminService roleAdminService;
  private final RoleMapper roleMapper;

  public RoleDataFetcher(
      RoleAdminService roleAdminService,
      RoleMapper roleMapper) {
    this.roleAdminService = roleAdminService;
    this.roleMapper = roleMapper;
  }

  /**
   * Lists all roles.
   */
  @DgsData(parentType = "GaiaQueries", field = "roles")
  @PreAuthorize("@permissions.has('graphql.gaia.roles')")
  @Transactional(readOnly = true)
  public List<Role> roles() {
    return roleAdminService.listRoles().stream()
        .map(roleMapper::map)
        .toList();
  }

   /**
    * Locates a role by id.
    */
  @DgsData(parentType = "GaiaQueries", field = "role")
  @PreAuthorize("@permissions.has('graphql.gaia.role')")
  @Transactional(readOnly = true)
  public Role role(String id) {
    return roleAdminService.findRoleById(UUID.fromString(id))
        .map(roleMapper::map)
        .orElse(null);
  }

   /**
    * Returns direct role names for the account.
    */
  @DgsData(parentType = "GaiaQueries", field = "accountRoles")
  @PreAuthorize("@permissions.has('graphql.gaia.accountRoles')")
  @Transactional(readOnly = true)
  public List<String> accountRoles(String accountId) {
    return roleAdminService.accountRoleNames(UUID.fromString(accountId));
  }

   /**
    * Creates a new role.
    */
  @DgsData(parentType = "GaiaMutations", field = "createRole")
  @PreAuthorize("@permissions.has('graphql.gaia.createRole')")
  @Transactional
  public CreateRoleResponse createRole(String name, String description) {
    Role role = roleMapper.map(roleAdminService.createRole(name, description));
    logger.info("Created role {}", name);
    return CreateRoleResponse.newBuilder()
        .message("Role created")
        .role(role)
        .build();
  }

   /**
    * Deletes a role.
    */
  @DgsData(parentType = "GaiaMutations", field = "deleteRole")
  @PreAuthorize("@permissions.has('graphql.gaia.deleteRole')")
  @Transactional
  public DeleteRoleResponse deleteRole(String id) {
    roleAdminService.deleteRole(UUID.fromString(id));
    logger.info("Deleted role {}", id);
    return DeleteRoleResponse.newBuilder()
        .message("Role deleted")
        .id(id)
        .build();
  }

   /**
    * Replaces the account's roles with the given set.
    */
  @DgsData(parentType = "GaiaMutations", field = "setAccountRoles")
  @PreAuthorize("@permissions.has('graphql.gaia.setAccountRoles')")
  @Transactional
  public SetAccountRolesResponse setAccountRoles(String accountId, List<String> roleNames) {
    roleAdminService.setAccountRoles(UUID.fromString(accountId), roleNames);
    logger.info("Set roles for account {}", accountId);
    return SetAccountRolesResponse.newBuilder()
        .message("Account roles updated")
        .accountId(accountId)
        .roles(roleNames)
        .build();
  }
}

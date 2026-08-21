package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.graphql.services.PermissionGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for permission operations.
 */
@DgsComponent
public class PermissionDataFetcher {

  private final PermissionGraphQLService permissionGraphQLService;

  public PermissionDataFetcher(PermissionGraphQLService permissionGraphQLService) {
    this.permissionGraphQLService = permissionGraphQLService;
  }

  /**
   * Returns the caller's role key strings, or empty when unauthenticated.
   */
  @DgsData(parentType = "GaiaQueries", field = "myRoles")
  @PreAuthorize("permitAll()")
  public List<String> myRoles() {
    return permissionGraphQLService.myRoles();
  }

   /**
    * Returns a remote account's effective permission patterns.
    */
  @DgsData(parentType = "GaiaQueries", field = "effectivePermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.effectivePermissions')")
  public List<String> effectivePermissions(
      RemoteUserType remoteUserType, String remoteUserId) {
    return permissionGraphQLService.effectivePermissions(remoteUserType, remoteUserId);
  }

   /**
    * Returns direct permission strings for the account.
    */
  @DgsData(parentType = "GaiaQueries", field = "accountPermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.accountPermissions')")
  public List<String> accountPermissions(String accountId) {
    return permissionGraphQLService.accountPermissions(accountId);
  }

   /**
    * Returns permission strings for the role.
    */
  @DgsData(parentType = "GaiaQueries", field = "rolePermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.rolePermissions')")
  public List<String> rolePermissions(String roleId) {
    return permissionGraphQLService.rolePermissions(roleId);
  }

   /**
    * Replaces the account's direct permissions with the given set.
    */
  @DgsData(parentType = "GaiaMutations", field = "setAccountPermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.setAccountPermissions')")
  public QueryResult setAccountPermissions(String accountId, List<String> permissions) {
    return permissionGraphQLService.setAccountPermissions(accountId, permissions);
  }

   /**
    * Replaces a role's permissions with the given set.
    */
  @DgsData(parentType = "GaiaMutations", field = "setRolePermissions")
  @PreAuthorize("@permissions.has('graphql.gaia.setRolePermissions')")
  public QueryResult setRolePermissions(String roleId, List<String> permissions) {
    return permissionGraphQLService.setRolePermissions(roleId, permissions);
  }
}

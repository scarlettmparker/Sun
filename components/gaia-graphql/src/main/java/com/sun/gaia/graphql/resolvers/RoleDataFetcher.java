package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.Role;
import com.sun.gaia.graphql.services.RoleGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for role operations.
 */
@DgsComponent
public class RoleDataFetcher {

  private final RoleGraphQLService roleGraphQLService;

  public RoleDataFetcher(RoleGraphQLService roleGraphQLService) {
    this.roleGraphQLService = roleGraphQLService;
  }

  /**
   * Lists all roles.
   */
  @DgsData(parentType = "GaiaQueries", field = "roles")
  @PreAuthorize("@permissions.has('graphql.gaia.roles')")
  public List<Role> roles() {
    return roleGraphQLService.roles();
  }

   /**
    * Locates a role by id.
    */
  @DgsData(parentType = "GaiaQueries", field = "role")
  @PreAuthorize("@permissions.has('graphql.gaia.role')")
  public Role role(String id) {
    return roleGraphQLService.role(id);
  }

   /**
    * Returns direct role names for the account.
    */
  @DgsData(parentType = "GaiaQueries", field = "accountRoles")
  @PreAuthorize("@permissions.has('graphql.gaia.accountRoles')")
  public List<String> accountRoles(String accountId) {
    return roleGraphQLService.accountRoles(accountId);
  }

   /**
    * Creates a new role.
    */
  @DgsData(parentType = "GaiaMutations", field = "createRole")
  @PreAuthorize("@permissions.has('graphql.gaia.createRole')")
  public Role createRole(String name, String description) {
    return roleGraphQLService.createRole(name, description);
  }

   /**
    * Deletes a role.
    */
  @DgsData(parentType = "GaiaMutations", field = "deleteRole")
  @PreAuthorize("@permissions.has('graphql.gaia.deleteRole')")
  public QueryResult deleteRole(String id) {
    return roleGraphQLService.deleteRole(id);
  }

   /**
    * Replaces the account's roles with the given set.
    */
  @DgsData(parentType = "GaiaMutations", field = "setAccountRoles")
  @PreAuthorize("@permissions.has('graphql.gaia.setAccountRoles')")
  public QueryResult setAccountRoles(String accountId, List<String> roleNames) {
    return roleGraphQLService.setAccountRoles(accountId, roleNames);
  }
}

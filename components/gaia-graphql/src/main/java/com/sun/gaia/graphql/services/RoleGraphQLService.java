package com.sun.gaia.graphql.services;

import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.Role;
import com.sun.gaia.graphql.mappers.RoleMapper;
import com.sun.gaia.service.RoleAdminService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for roles.
 */
@Service
public class RoleGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(RoleGraphQLService.class);

  private final RoleAdminService roleAdminService;
  private final RoleMapper roleMapper;

  public RoleGraphQLService(
      RoleAdminService roleAdminService,
      RoleMapper roleMapper) {
    this.roleAdminService = roleAdminService;
    this.roleMapper = roleMapper;
  }

  /**
   * Lists all roles ordered by name.
   *
   * @return the roles
   */
  @Transactional(readOnly = true)
  public List<Role> roles() {
    return roleAdminService.listRoles().stream()
        .map(roleMapper::map)
        .toList();
  }

  /**
   * Locates a role by id.
   *
   * @param id the role id
   * @return the role, or null when absent
   */
  @Transactional(readOnly = true)
  public Role role(String id) {
    return roleAdminService.findRoleById(UUID.fromString(id))
        .map(roleMapper::map)
        .orElse(null);
  }

  /**
   * Returns direct role names for the account.
   *
   * @param accountId the account id
   * @return the role names
   */
  @Transactional(readOnly = true)
  public List<String> accountRoles(String accountId) {
    return roleAdminService.accountRoleNames(UUID.fromString(accountId));
  }

  /**
   * Creates a new role.
   *
   * @param name the role name
   * @param description the optional description
   * @return the created role
   */
  @Transactional
  public Role createRole(String name, String description) {
    return roleMapper.map(roleAdminService.createRole(name, description));
  }

  /**
   * Deletes a role and its assignments.
   *
   * @param id the role id
   * @return a success result
   */
  @Transactional
  public QueryResult deleteRole(String id) {
    roleAdminService.deleteRole(UUID.fromString(id));
    logger.info("Deleted role {}", id);
    return QuerySuccess.newBuilder().message("Role deleted").id(id).build();
  }

  /**
   * Replaces the account's roles with the given set.
   *
   * @param accountId the account id
   * @param roleNames the desired role names
   * @return a success result
   */
  @Transactional
  public QueryResult setAccountRoles(String accountId, List<String> roleNames) {
    roleAdminService.setAccountRoles(UUID.fromString(accountId), roleNames);
    logger.info("Set roles for account {}", accountId);
    return QuerySuccess.newBuilder().message("Account roles updated").id(accountId).build();
  }
}

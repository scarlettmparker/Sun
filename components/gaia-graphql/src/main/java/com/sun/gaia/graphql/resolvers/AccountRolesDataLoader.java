package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsDataLoader;
import com.sun.gaia.service.RoleAdminService;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.dataloader.MappedBatchLoader;

/**
 * Batch-resolves account role names, one query per GraphQL request regardless
 * of how many accounts are listed.
 */
@DgsDataLoader(name = "accountRoles")
public class AccountRolesDataLoader implements MappedBatchLoader<String, List<String>> {

  private final RoleAdminService roleAdminService;

  public AccountRolesDataLoader(RoleAdminService roleAdminService) {
    this.roleAdminService = roleAdminService;
  }

  @Override
  public CompletableFuture<Map<String, List<String>>> load(Set<String> accountIds) {
    Map<String, List<String>> byAccount = new HashMap<>();
    if (accountIds == null || accountIds.isEmpty()) {
      return CompletableFuture.completedFuture(byAccount);
    }

    Set<UUID> ids = new LinkedHashSet<>();
    for (String accountId : accountIds) {
      try {
        ids.add(UUID.fromString(accountId));
      } catch (IllegalArgumentException ignored) {
        // Unknown ids resolve to an empty role list below.
      }
    }

    Map<UUID, List<String>> resolved = roleAdminService.accountRoleNamesBatch(ids);
    for (String accountId : accountIds) {
      List<String> names = null;
      try {
        names = resolved.get(UUID.fromString(accountId));
      } catch (IllegalArgumentException ignored) {
        // Fall through to the empty list.
      }
      byAccount.put(accountId, names == null ? List.of() : names);
    }
    return CompletableFuture.completedFuture(byAccount);
  }
}

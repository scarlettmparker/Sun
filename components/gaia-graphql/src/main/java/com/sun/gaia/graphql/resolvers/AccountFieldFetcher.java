package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.sun.base.audit.annotation.NotAudited;
import com.sun.gaia.codegen.types.Account;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.dataloader.DataLoader;

/**
 * Attaches the caller-visible role names to an account, batched by the
 * accountRoles DataLoader.
 */
@DgsComponent
public class AccountFieldFetcher {

  /**
   * Resolves the direct roles for an account.
   *
   * @param env DGS fetching environment
   * @return role names, empty when the account holds none
   */
  @NotAudited(reason = "field resolver")
  @DgsData(parentType = "Account", field = "roles")
  public CompletableFuture<List<String>> roles(DgsDataFetchingEnvironment env) {
    Account source = env.getSource();
    DataLoader<String, List<String>> loader = env.getDataLoader("accountRoles");
    return loader.load(source.getId());
  }
}

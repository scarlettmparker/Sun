package com.sun.hades.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.hades.codegen.types.DiscordLoginResult;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.codegen.types.ReaderObjectReference;
import com.sun.hades.codegen.types.RemoteUserInput;
import com.sun.hades.graphql.services.ReaderAccountGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for the reader queries and mutations.
 */
@DgsComponent
public class ReaderAccountDataFetcher {

  private final ReaderAccountGraphQLService readerAccountGraphQLService;

  public ReaderAccountDataFetcher(ReaderAccountGraphQLService readerAccountGraphQLService) {
    this.readerAccountGraphQLService = readerAccountGraphQLService;
  }

  /**
   * Returns the current member's reader account, or null when unauthenticated.
   *
   * @return the reader account
   */
  @DgsData(parentType = "HadesQueries", field = "readerAccount")
  @PreAuthorize("permitAll()")
  public ReaderAccount readerAccount() {
    return readerAccountGraphQLService.readerAccount();
  }

  /**
   * Locates reader accounts for a set of remote users.
   *
   * @param remoteUsers the remote-user references
   * @return the matching reader accounts
   */
  @DgsData(parentType = "HadesQueries", field = "readerAccounts")
  @PreAuthorize("@permissions.has('graphql.hades.readerAccounts')")
  public List<ReaderAccount> readerAccounts(
      List<RemoteUserInput> remoteUsers) {
    return readerAccountGraphQLService.readerAccounts(remoteUsers);
  }

  /**
   * Searches reader accounts by username.
   *
   * @param query the username fragment
   * @param pagination the page request
   * @return the matching reader accounts
   */
  @DgsData(parentType = "HadesQueries", field = "searchReaderAccounts")
  @PreAuthorize("@permissions.has('graphql.hades.searchReaderAccounts')")
  public List<ReaderAccount> searchReaderAccounts(String query, PaginationInput pagination) {
    return readerAccountGraphQLService.searchReaderAccounts(query, pagination);
  }

  /**
   * Finds annotations referencing any of the given remote object ids.
   *
   * @param ids the remote object ids
   * @return the references
   */
  @DgsData(parentType = "HadesQueries", field = "locateRemoteObjects")
  @PreAuthorize("@permissions.has('graphql.hades.locateRemoteObjects')")
  public List<ReaderObjectReference> locateRemoteObjects(List<String> ids) {
    return readerAccountGraphQLService.locateRemoteObjects(ids);
  }

  /**
   * Exchanges a Discord authorization code for a JWT.
   *
   * @param code the authorization code
   * @param state the OAuth state token
   * @return the login result
   */
  @DgsData(parentType = "HadesMutations", field = "discordLogin")
  @PreAuthorize("permitAll()")
  public DiscordLoginResult discordLogin(String code, String state) {
    return readerAccountGraphQLService.discordLogin(code, state);
  }
}

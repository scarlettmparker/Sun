package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.IpWhitelistEntry;
import com.sun.gaia.codegen.types.IpWhitelistEntryInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.graphql.services.IpWhitelistGraphQLService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for IP whitelist operations.
 */
@DgsComponent
public class IpWhitelistDataFetcher {

  private final IpWhitelistGraphQLService ipWhitelistGraphQLService;

  public IpWhitelistDataFetcher(IpWhitelistGraphQLService ipWhitelistGraphQLService) {
    this.ipWhitelistGraphQLService = ipWhitelistGraphQLService;
  }

  /**
   * Lists all IP whitelist entries.
   *
   * @return the entries
   */
  @DgsData(parentType = "GaiaQueries", field = "ipWhitelistEntries")
  @PreAuthorize("@permissions.has('graphql.gaia.ipWhitelistEntries')")
  public List<IpWhitelistEntry> ipWhitelistEntries() {
    return ipWhitelistGraphQLService.ipWhitelistEntries();
  }

  /**
   * Creates a new IP whitelist entry.
   *
   * @param pattern     the IP pattern
   * @param description optional description
   * @return a success result
   */
  @DgsData(parentType = "GaiaMutations", field = "createIpWhitelistEntry")
  @PreAuthorize("@permissions.has('graphql.gaia.createIpWhitelistEntry')")
  public QueryResult createIpWhitelistEntry(IpWhitelistEntryInput input) {
    return ipWhitelistGraphQLService.createIpWhitelistEntry(input);
  }

  /**
   * Updates an existing IP whitelist entry.
   *
   * @param id    the entry id
   * @param input the updated fields
   * @return a success result
   */
  @DgsData(parentType = "GaiaMutations", field = "updateIpWhitelistEntry")
  @PreAuthorize("@permissions.has('graphql.gaia.updateIpWhitelistEntry')")
  public QueryResult updateIpWhitelistEntry(String id, IpWhitelistEntryInput input) {
    return ipWhitelistGraphQLService.updateIpWhitelistEntry(id, input);
  }

  /**
   * Deletes an IP whitelist entry.
   *
   * @param id the entry id
   * @return a success result
   */
  @DgsData(parentType = "GaiaMutations", field = "deleteIpWhitelistEntry")
  @PreAuthorize("@permissions.has('graphql.gaia.deleteIpWhitelistEntry')")
  public QueryResult deleteIpWhitelistEntry(String id) {
    return ipWhitelistGraphQLService.deleteIpWhitelistEntry(id);
  }
}

package com.sun.gaia.graphql.services;

import com.sun.gaia.codegen.types.IpWhitelistEntry;
import com.sun.gaia.codegen.types.IpWhitelistEntryInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.graphql.mappers.IpWhitelistMapper;
import com.sun.gaia.model.IpWhitelistEntryEntity;
import com.sun.gaia.service.IpWhitelistService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for IP whitelist entries.
 */
@Service
public class IpWhitelistGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(IpWhitelistGraphQLService.class);

  private final IpWhitelistService ipWhitelistService;
  private final IpWhitelistMapper ipWhitelistMapper;

  public IpWhitelistGraphQLService(
      IpWhitelistService ipWhitelistService,
      IpWhitelistMapper ipWhitelistMapper) {
    this.ipWhitelistService = ipWhitelistService;
    this.ipWhitelistMapper = ipWhitelistMapper;
  }

  /**
   * Lists all IP whitelist entries.
   *
   * @return the list of entries
   */
  @Transactional(readOnly = true)
  public List<IpWhitelistEntry> ipWhitelistEntries() {
    return ipWhitelistMapper.map(ipWhitelistService.listAll());
  }

  /**
   * Creates a new IP whitelist entry.
   *
   * @param pattern     the IP pattern (CIDR, glob, or exact).
   * @param description optional description.
   * @return a success result with the entry id
   */
  @Transactional
  public QueryResult createIpWhitelistEntry(IpWhitelistEntryInput input) {
    IpWhitelistEntryEntity entity = ipWhitelistService.addEntry(
        input.getPattern(), input.getDescription(),
        input.getImmutable() != null && input.getImmutable());
    return QuerySuccess.newBuilder()
        .message("IP whitelist entry created")
        .id(entity.getId().toString())
        .build();
  }

  /**
   * Updates an existing IP whitelist entry.
   *
   * @param id    the entry id
   * @param input the updated fields
   * @return a success result
   */
  @Transactional
  public QueryResult updateIpWhitelistEntry(String id, IpWhitelistEntryInput input) {
    ipWhitelistService.updateEntry(UUID.fromString(id),
        input.getPattern(), input.getDescription(), input.getEnabled());
    return QuerySuccess.newBuilder()
        .message("IP whitelist entry updated")
        .id(id)
        .build();
  }

  /**
   * Deletes an IP whitelist entry.
   *
   * @param id the entry id
   * @return a success result
   */
  @Transactional
  public QueryResult deleteIpWhitelistEntry(String id) {
    ipWhitelistService.deleteEntry(UUID.fromString(id));
    return QuerySuccess.newBuilder()
        .message("IP whitelist entry deleted")
        .id(id)
        .build();
  }
}

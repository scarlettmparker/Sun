package com.sun.gaia.graphql.services;

import com.sun.gaia.codegen.types.CreateIpWhitelistEntryResponse;
import com.sun.gaia.codegen.types.DeleteIpWhitelistEntryResponse;
import com.sun.gaia.codegen.types.IpWhitelistEntry;
import com.sun.gaia.codegen.types.IpWhitelistEntryInput;
import com.sun.gaia.codegen.types.UpdateIpWhitelistEntryResponse;
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
   * @return the response with the created entry
   */
  @Transactional
  public CreateIpWhitelistEntryResponse createIpWhitelistEntry(IpWhitelistEntryInput input) {
    IpWhitelistEntryEntity entity = ipWhitelistService.addEntry(
        input.getPattern(), input.getDescription(),
        input.getImmutable() != null && input.getImmutable());
    return CreateIpWhitelistEntryResponse.newBuilder()
        .message("IP whitelist entry created")
        .entry(ipWhitelistMapper.map(entity))
        .build();
  }

  /**
   * Updates an existing IP whitelist entry.
   *
   * @param id    the entry id
   * @param input the updated fields
   * @return the response with the updated entry
   */
  @Transactional
  public UpdateIpWhitelistEntryResponse updateIpWhitelistEntry(String id, IpWhitelistEntryInput input) {
    IpWhitelistEntryEntity entity = ipWhitelistService.updateEntry(UUID.fromString(id),
        input.getPattern(), input.getDescription(), input.getEnabled());
    return UpdateIpWhitelistEntryResponse.newBuilder()
        .message("IP whitelist entry updated")
        .entry(ipWhitelistMapper.map(entity))
        .build();
  }

  /**
   * Deletes an IP whitelist entry.
   *
   * @param id the entry id
   * @return the deletion result
   */
  @Transactional
  public DeleteIpWhitelistEntryResponse deleteIpWhitelistEntry(String id) {
    ipWhitelistService.deleteEntry(UUID.fromString(id));
    return DeleteIpWhitelistEntryResponse.newBuilder()
        .message("IP whitelist entry deleted")
        .id(id)
        .build();
  }
}

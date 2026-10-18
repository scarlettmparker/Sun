package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.IssueApiKeyResponse;
import com.sun.gaia.codegen.types.IssuedApiKey;
import com.sun.gaia.codegen.types.RevokeApiKeyResponse;
import com.sun.gaia.codegen.types.RotateApiKeyResponse;
import com.sun.gaia.graphql.mappers.ApiKeyMapper;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.ApiKeyService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data fetchers for API key operations.
 */
@DgsComponent
public class ApiKeyDataFetcher {

  private static final Logger logger = LoggerFactory.getLogger(ApiKeyDataFetcher.class);

  private final AccountService accountService;
  private final ApiKeyService apiKeyService;
  private final ApiKeyMapper apiKeyMapper;

  public ApiKeyDataFetcher(
      AccountService accountService,
      ApiKeyService apiKeyService,
      ApiKeyMapper apiKeyMapper) {
    this.accountService = accountService;
    this.apiKeyService = apiKeyService;
    this.apiKeyMapper = apiKeyMapper;
  }

  /**
   * Issues a new API key for an account.
   *
   * @param accountUsername the account username
   * @param name            the key label
   * @return the issued key and its one-time plaintext
   */
  @DgsData(parentType = "GaiaMutations", field = "issueApiKey")
  @PreAuthorize("@permissions.has('graphql.gaia.issueApiKey')")
  @Transactional
  public IssueApiKeyResponse issueApiKey(String accountUsername, String name) {
    AccountEntity account = accountService.findByUsername(accountUsername)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountUsername));
    ApiKeyService.ApiKeyIssue issue = apiKeyService.issueKey(account.getId(), name);
    logger.info("Issued API key {} for account {}", issue.apiKey().getId(), account.getId());
    return IssueApiKeyResponse.newBuilder()
        .message("API key issued")
        .apiKey(IssuedApiKey.newBuilder()
            .apiKey(apiKeyMapper.map(issue.apiKey()))
            .plaintextKey(issue.plaintextKey())
            .build())
        .build();
  }

  /**
   * Disables an API key.
   *
   * @param id the key id
   * @return a success result
   */
  @DgsData(parentType = "GaiaMutations", field = "revokeApiKey")
  @PreAuthorize("@permissions.has('graphql.gaia.revokeApiKey')")
  @Transactional
  public RevokeApiKeyResponse revokeApiKey(String id) {
    apiKeyService.revoke(UUID.fromString(id));
    logger.info("Revoked API key {}", id);
    return RevokeApiKeyResponse.newBuilder()
        .message("API key revoked")
        .id(id)
        .build();
  }

  /**
   * Issues a fresh plaintext for an existing API key.
   *
   * @param id the key id
   * @return the rotated key and its one-time plaintext
   */
  @DgsData(parentType = "GaiaMutations", field = "rotateApiKey")
  @PreAuthorize("@permissions.has('graphql.gaia.rotateApiKey')")
  @Transactional
  public RotateApiKeyResponse rotateApiKey(String id) {
    ApiKeyService.ApiKeyIssue issue = apiKeyService.rotate(UUID.fromString(id));
    logger.info("Rotated API key {}", id);
    return RotateApiKeyResponse.newBuilder()
        .message("API key rotated")
        .apiKey(IssuedApiKey.newBuilder()
            .apiKey(apiKeyMapper.map(issue.apiKey()))
            .plaintextKey(issue.plaintextKey())
            .build())
        .build();
  }
}

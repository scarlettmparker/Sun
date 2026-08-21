package com.sun.gaia.graphql.services;

import com.sun.gaia.codegen.types.IssuedApiKey;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.graphql.mappers.ApiKeyMapper;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.ApiKeyService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for API keys.
 */
@Service
public class ApiKeyGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(ApiKeyGraphQLService.class);

  private final AccountService accountService;
  private final ApiKeyService apiKeyService;
  private final ApiKeyMapper apiKeyMapper;

  public ApiKeyGraphQLService(
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
  @Transactional
  public IssuedApiKey issueApiKey(String accountUsername, String name) {
    AccountEntity account = accountService.findByUsername(accountUsername)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountUsername));
    ApiKeyService.ApiKeyIssue issue = apiKeyService.issueKey(account.getId(), name);
    logger.info("Issued API key {} for account {}", issue.apiKey().getId(), account.getId());
    return IssuedApiKey.newBuilder()
        .apiKey(apiKeyMapper.map(issue.apiKey()))
        .plaintextKey(issue.plaintextKey())
        .build();
  }

  /**
   * Disables an API key.
   *
   * @param id the key id
   * @return a success result
   */
  @Transactional
  public QueryResult revokeApiKey(String id) {
    apiKeyService.revoke(UUID.fromString(id));
    logger.info("Revoked API key {}", id);
    return QuerySuccess.newBuilder().message("API key revoked").id(id).build();
  }

  /**
   * Issues a fresh plaintext for an existing API key.
   *
   * @param id the key id
   * @return the rotated key and its one-time plaintext
   */
  @Transactional
  public IssuedApiKey rotateApiKey(String id) {
    ApiKeyService.ApiKeyIssue issue = apiKeyService.rotate(UUID.fromString(id));
    logger.info("Rotated API key {}", id);
    return IssuedApiKey.newBuilder()
        .apiKey(apiKeyMapper.map(issue.apiKey()))
        .plaintextKey(issue.plaintextKey())
        .build();
  }
}

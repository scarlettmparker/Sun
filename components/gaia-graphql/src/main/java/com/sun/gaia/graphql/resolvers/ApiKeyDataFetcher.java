package com.sun.gaia.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.gaia.codegen.types.IssueApiKeyResponse;
import com.sun.gaia.codegen.types.RevokeApiKeyResponse;
import com.sun.gaia.codegen.types.RotateApiKeyResponse;
import com.sun.gaia.graphql.services.ApiKeyGraphQLService;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Data fetchers for API key operations.
 */
@DgsComponent
public class ApiKeyDataFetcher {

  private final ApiKeyGraphQLService apiKeyGraphQLService;

  public ApiKeyDataFetcher(ApiKeyGraphQLService apiKeyGraphQLService) {
    this.apiKeyGraphQLService = apiKeyGraphQLService;
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
  public IssueApiKeyResponse issueApiKey(String accountUsername, String name) {
    return apiKeyGraphQLService.issueApiKey(accountUsername, name);
  }

  /**
   * Disables an API key.
   *
   * @param id the key id
   * @return a success result
   */
  @DgsData(parentType = "GaiaMutations", field = "revokeApiKey")
  @PreAuthorize("@permissions.has('graphql.gaia.revokeApiKey')")
  public RevokeApiKeyResponse revokeApiKey(String id) {
    return apiKeyGraphQLService.revokeApiKey(id);
  }

  /**
   * Issues a fresh plaintext for an existing API key.
   *
   * @param id the key id
   * @return the rotated key and its one-time plaintext
   */
  @DgsData(parentType = "GaiaMutations", field = "rotateApiKey")
  @PreAuthorize("@permissions.has('graphql.gaia.rotateApiKey')")
  public RotateApiKeyResponse rotateApiKey(String id) {
    return apiKeyGraphQLService.rotateApiKey(id);
  }
}

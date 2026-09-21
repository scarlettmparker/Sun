package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.IssueApiKeyResponse;
import com.sun.gaia.codegen.types.IssuedApiKey;
import com.sun.gaia.codegen.types.RevokeApiKeyResponse;
import com.sun.gaia.codegen.types.RotateApiKeyResponse;
import com.sun.gaia.graphql.services.ApiKeyGraphQLService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyDataFetcherTest {

  @Mock private ApiKeyGraphQLService service;

  @InjectMocks private ApiKeyDataFetcher fetcher;

  @Test
  void issueApiKey_shouldDelegate() {
    IssueApiKeyResponse mock = IssueApiKeyResponse.newBuilder()
        .message("API key issued")
        .apiKey(IssuedApiKey.newBuilder().plaintextKey("ns_plain").build())
        .build();
    when(service.issueApiKey("alice", "bot")).thenReturn(mock);

    IssueApiKeyResponse result = fetcher.issueApiKey("alice", "bot");

    assertThat(result).isEqualTo(mock);
    verify(service).issueApiKey("alice", "bot");
  }

  @Test
  void revokeApiKey_shouldDelegate() {
    RevokeApiKeyResponse mock = RevokeApiKeyResponse.newBuilder()
        .message("API key revoked")
        .id("id1")
        .build();
    when(service.revokeApiKey("id1")).thenReturn(mock);

    RevokeApiKeyResponse result = fetcher.revokeApiKey("id1");

    assertThat(result).isEqualTo(mock);
    verify(service).revokeApiKey("id1");
  }

  @Test
  void rotateApiKey_shouldDelegate() {
    RotateApiKeyResponse mock = RotateApiKeyResponse.newBuilder()
        .message("API key rotated")
        .apiKey(IssuedApiKey.newBuilder().plaintextKey("ns_rotated").build())
        .build();
    when(service.rotateApiKey("id1")).thenReturn(mock);

    RotateApiKeyResponse result = fetcher.rotateApiKey("id1");

    assertThat(result).isEqualTo(mock);
    verify(service).rotateApiKey("id1");
  }
}

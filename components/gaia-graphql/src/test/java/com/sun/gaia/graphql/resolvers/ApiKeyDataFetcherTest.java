package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.IssuedApiKey;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
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
    IssuedApiKey mock = IssuedApiKey.newBuilder().plaintextKey("ns_plain").build();
    when(service.issueApiKey("alice", "bot")).thenReturn(mock);

    IssuedApiKey result = fetcher.issueApiKey("alice", "bot");

    assertThat(result).isEqualTo(mock);
    verify(service).issueApiKey("alice", "bot");
  }

  @Test
  void revokeApiKey_shouldDelegate() {
    QueryResult mock = QuerySuccess.newBuilder().message("API key revoked").id("id1").build();
    when(service.revokeApiKey("id1")).thenReturn(mock);

    QueryResult result = fetcher.revokeApiKey("id1");

    assertThat(result).isEqualTo(mock);
    verify(service).revokeApiKey("id1");
  }

  @Test
  void rotateApiKey_shouldDelegate() {
    IssuedApiKey mock = IssuedApiKey.newBuilder().plaintextKey("ns_rotated").build();
    when(service.rotateApiKey("id1")).thenReturn(mock);

    IssuedApiKey result = fetcher.rotateApiKey("id1");

    assertThat(result).isEqualTo(mock);
    verify(service).rotateApiKey("id1");
  }
}

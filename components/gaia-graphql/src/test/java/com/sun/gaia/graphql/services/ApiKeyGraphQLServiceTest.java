package com.sun.gaia.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.ApiKey;
import com.sun.gaia.codegen.types.IssuedApiKey;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.graphql.mappers.ApiKeyMapper;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.ApiKeyEntity;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.ApiKeyService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyGraphQLServiceTest {

  @Mock private AccountService accountService;
  @Mock private ApiKeyService apiKeyService;
  @Mock private ApiKeyMapper apiKeyMapper;

  @InjectMocks private ApiKeyGraphQLService service;

  @Test
  void issueApiKey_returnsIssuedKeyWithPlaintext() {
    UUID accountId = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(accountId);
    when(accountService.findByUsername("niece-scarlett")).thenReturn(Optional.of(account));
    ApiKeyEntity key = new ApiKeyEntity();
    key.setId(UUID.randomUUID());
    when(apiKeyService.issueKey(accountId, "bot")).thenReturn(
        new ApiKeyService.ApiKeyIssue(key, "ns_plaintext"));
    ApiKey mapped = ApiKey.newBuilder().id(key.getId().toString()).name("bot").build();
    when(apiKeyMapper.map(key)).thenReturn(mapped);

    IssuedApiKey result = service.issueApiKey("niece-scarlett", "bot");

    assertThat(result.getPlaintextKey()).isEqualTo("ns_plaintext");
    assertThat(result.getApiKey()).isEqualTo(mapped);
    verify(apiKeyService).issueKey(accountId, "bot");
  }

  @Test
  void issueApiKey_throwsWhenAccountNotFound() {
    when(accountService.findByUsername("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.issueApiKey("missing", "bot"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void revokeApiKey_returnsQuerySuccess() {
    UUID id = UUID.randomUUID();

    QueryResult result = service.revokeApiKey(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    verify(apiKeyService).revoke(id);
  }

  @Test
  void revokeApiKey_throwsWhenKeyNotFound() {
    UUID id = UUID.randomUUID();
    doThrow(new IllegalArgumentException("API key not found: " + id)).when(apiKeyService).revoke(id);

    assertThatThrownBy(() -> service.revokeApiKey(id.toString()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rotateApiKey_returnsRotatedPlaintext() {
    UUID id = UUID.randomUUID();
    ApiKeyEntity key = new ApiKeyEntity();
    key.setId(id);
    when(apiKeyService.rotate(id)).thenReturn(
        new ApiKeyService.ApiKeyIssue(key, "ns_rotated"));
    ApiKey mapped = ApiKey.newBuilder().id(key.getId().toString()).name("bot").build();
    when(apiKeyMapper.map(key)).thenReturn(mapped);

    IssuedApiKey result = service.rotateApiKey(id.toString());

    assertThat(result.getPlaintextKey()).isEqualTo("ns_rotated");
    assertThat(result.getApiKey()).isEqualTo(mapped);
    verify(apiKeyService).rotate(id);
  }

  @Test
  void rotateApiKey_throwsWhenKeyNotFound() {
    UUID id = UUID.randomUUID();
    when(apiKeyService.rotate(id)).thenThrow(new IllegalArgumentException("API key not found: " + id));

    assertThatThrownBy(() -> service.rotateApiKey(id.toString()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

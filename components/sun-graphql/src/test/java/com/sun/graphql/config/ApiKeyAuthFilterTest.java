package com.sun.graphql.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.ApiKeyEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.ApiKeyService;
import com.sun.gaia.service.UserContextHolder;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyAuthFilterTest {

  private ApiKeyService apiKeyService;
  private AccountRepository accountRepository;
  private ApiKeyAuthFilter filter;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    apiKeyService = mock(ApiKeyService.class);
    accountRepository = mock(AccountRepository.class);
    filter = new ApiKeyAuthFilter(apiKeyService, accountRepository);
    chain = mock(FilterChain.class);
  }

  @AfterEach
  void tearDown() {
    UserContextHolder.clear();
  }

  @Test
  void validKey_authenticatesAndContinuesChain() throws Exception {
    UUID accountId = UUID.randomUUID();
    ApiKeyEntity key = new ApiKeyEntity();
    key.setId(UUID.randomUUID());
    key.setAccountId(accountId);
    when(apiKeyService.resolve("ns_plaintext")).thenReturn(Optional.of(key));
    AccountEntity account = new AccountEntity();
    account.setId(accountId);
    account.setStatus(AccountStatus.ACTIVE);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    AtomicReference<UUID> contextAtChain = new AtomicReference<>();
    doAnswer(invocation -> {
      contextAtChain.set(UserContextHolder.getUserId());
      return null;
    }).when(chain).doFilter(any(), any());

    MockHttpServletRequest request = requestWithKey("ns_plaintext");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(contextAtChain.get()).isEqualTo(accountId);
    verify(chain).doFilter(any(), any());
    verify(apiKeyService).markUsed(key.getId());
  }

  @Test
  void unknownKey_rejectsWith401() throws Exception {
    when(apiKeyService.resolve("ns_unknown")).thenReturn(Optional.empty());

    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(requestWithKey("ns_unknown"), response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void revokedKey_rejectsWith401() throws Exception {
    ApiKeyEntity key = new ApiKeyEntity();
    key.setAccountId(UUID.randomUUID());
    key.setEnabled(false);
    when(apiKeyService.resolve("ns_revoked")).thenReturn(Optional.of(key));

    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(requestWithKey("ns_revoked"), response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    verify(chain, never()).doFilter(any(), any());
    verify(accountRepository, never()).findById(any());
  }

  @Test
  void inactiveAccount_rejectsWith401() throws Exception {
    UUID accountId = UUID.randomUUID();
    ApiKeyEntity key = new ApiKeyEntity();
    key.setAccountId(accountId);
    when(apiKeyService.resolve("ns_suspended")).thenReturn(Optional.of(key));
    AccountEntity account = new AccountEntity();
    account.setStatus(AccountStatus.SUSPENDED);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(requestWithKey("ns_suspended"), response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void jwtTakesPrecedenceOverApiKey() throws Exception {
    UUID jwtAccount = UUID.randomUUID();
    UserContextHolder.setUserId(jwtAccount);

    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(requestWithKey("ns_ignored"), response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(chain).doFilter(any(), any());
    verify(apiKeyService, never()).resolve(any());
  }

  @Test
  void missingKey_passesThroughUnauthenticated() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/graphql");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(chain).doFilter(any(), any());
    verify(apiKeyService, never()).resolve(any());
  }

  @Test
  void clearsUserContextAfterRequest() throws Exception {
    UUID accountId = UUID.randomUUID();
    ApiKeyEntity key = new ApiKeyEntity();
    key.setAccountId(accountId);
    when(apiKeyService.resolve("ns_plaintext")).thenReturn(Optional.of(key));
    AccountEntity account = new AccountEntity();
    account.setStatus(AccountStatus.ACTIVE);
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    filter.doFilter(requestWithKey("ns_plaintext"), new MockHttpServletResponse(), chain);

    assertThat(UserContextHolder.getUserId()).isNull();
  }

  private static MockHttpServletRequest requestWithKey(String key) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/graphql");
    request.addHeader("X-Api-Key", key);
    return request;
  }
}

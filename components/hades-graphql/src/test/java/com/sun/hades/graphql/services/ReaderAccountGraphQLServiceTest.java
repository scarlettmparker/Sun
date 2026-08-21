package com.sun.hades.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.model.enums.AccountType;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.codegen.types.DiscordLoginResult;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.codegen.types.ReaderObjectReference;
import com.sun.hades.codegen.types.RemoteUserInput;
import com.sun.hades.codegen.types.RemoteUserType;
import com.sun.hades.graphql.mappers.ReaderAccountMapper;
import com.sun.hades.graphql.mappers.ReaderObjectReferenceMapper;
import com.sun.hades.graphql.mappers.RemoteUserMapper;
import com.sun.hades.model.ReaderAccountEntity;
import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.service.DiscordOAuthService;
import com.sun.hades.service.PrivateNoteService;
import com.sun.hades.service.ReaderAccountService;
import com.sun.hades.service.ReaderAnnotationService;
import com.sun.hades.service.RemoteObjectReference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReaderAccountGraphQLServiceTest {

  @Mock private ReaderAccountService accountService;
  @Mock private ReaderAnnotationService annotationService;
  @Mock private PrivateNoteService privateNoteService;
  @Mock private DiscordOAuthService discordOAuthService;
  @Mock private AccountService gaiaAccountService;
  @Mock private JwtService jwtService;
  @Mock private ReaderAccountMapper accountMapper;
  @Mock private ReaderObjectReferenceMapper objectReferenceMapper;
  @Mock private RemoteUserMapper remoteUserMapper;

  @InjectMocks private ReaderAccountGraphQLService service;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUser() {
    UserContextHolder.setUserId(userId);
  }

  @AfterEach
  void clearUser() {
    UserContextHolder.clear();
  }

  @Test
  void readerAccount_returnsWhenAuthenticated() {
    ReaderAccountEntity entity = new ReaderAccountEntity();
    entity.setId(UUID.randomUUID());
    entity.setGaiaAccountId(userId);
    when(accountService.findByGaiaAccountId(userId)).thenReturn(Optional.of(entity));
    ReaderAccount mapped = ReaderAccount.newBuilder().id(entity.getId().toString()).build();
    when(accountMapper.map(entity)).thenReturn(mapped);

    ReaderAccount result = service.readerAccount();

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void readerAccount_returnsNullWhenUnauthenticated() {
    UserContextHolder.clear();

    assertThat(service.readerAccount()).isNull();
  }

  @Test
  void readerAccount_returnsNullWhenNotFound() {
    when(accountService.findByGaiaAccountId(userId)).thenReturn(Optional.empty());

    assertThat(service.readerAccount()).isNull();
  }

  @Test
  void readerAccounts_returnsFilteredByDiscord() {
    RemoteUserInput input = RemoteUserInput.newBuilder()
        .type(RemoteUserType.DISCORD).id("123").build();
    ReaderAccountEntity entity = new ReaderAccountEntity();
    entity.setId(UUID.randomUUID());
    entity.setDiscordId("123");
    when(accountService.findByDiscordIds(List.of("123"))).thenReturn(List.of(entity));
    ReaderAccount mapped = ReaderAccount.newBuilder().id(entity.getId().toString()).discordId("123").build();
    when(accountMapper.map(entity)).thenReturn(mapped);

    List<ReaderAccount> result = service.readerAccounts(List.of(input));

    assertThat(result).containsExactly(mapped);
  }

  @Test
  void readerAccounts_handlesNullInput() {
    when(accountService.findByDiscordIds(List.of())).thenReturn(List.of());

    List<ReaderAccount> result = service.readerAccounts(null);

    assertThat(result).isEmpty();
  }

  @Test
  void searchReaderAccounts_returnsFilteredResults() {
    ReaderAccountEntity entity = new ReaderAccountEntity();
    entity.setId(UUID.randomUUID());
    entity.setGaiaAccountId(UUID.randomUUID());
    when(accountService.searchByUsername(eq("test"), any())).thenReturn(List.of(entity));
    AccountEntity gaia = new AccountEntity();
    gaia.setId(entity.getGaiaAccountId());
    gaia.setAccountType(AccountType.HUMAN);
    when(gaiaAccountService.findById(entity.getGaiaAccountId())).thenReturn(Optional.of(gaia));
    ReaderAccount mapped = ReaderAccount.newBuilder().id(entity.getId().toString()).build();
    when(accountMapper.map(entity)).thenReturn(mapped);

    List<ReaderAccount> result = service.searchReaderAccounts("test", null);

    assertThat(result).containsExactly(mapped);
  }

  @Test
  void searchReaderAccounts_returnsEmptyWhenBlankQuery() {
    List<ReaderAccount> result = service.searchReaderAccounts("   ", null);

    assertThat(result).isEmpty();
  }

  @Test
  void searchReaderAccounts_returnsEmptyWhenNullQuery() {
    assertThat(service.searchReaderAccounts(null, null)).isEmpty();
  }

  @Test
  void locateRemoteObjects_delegates() {
    UUID id = UUID.randomUUID();
    RemoteObjectReference ref = new RemoteObjectReference(id, "ANNOTATION", id, null);
    when(annotationService.locateRemoteObjects(List.of("hades:1"))).thenReturn(List.of(ref));
    when(privateNoteService.locateRemoteObjects(List.of("hades:1"))).thenReturn(List.of());
    ReaderObjectReference mapped = ReaderObjectReference.newBuilder()
        .id(id.toString()).ownerType("ANNOTATION").build();
    when(objectReferenceMapper.map(ref)).thenReturn(mapped);

    List<ReaderObjectReference> result = service.locateRemoteObjects(List.of("hades:1"));

    assertThat(result).containsExactly(mapped);
  }

  @Test
  void discordLogin_returnsTokenWhenActive() {
    String code = "code";
    String state = "state";
    DiscordOAuthService.DiscordProfile profile = new DiscordOAuthService.DiscordProfile(
        "123", "user", "User", "avatar", "user@example.com", CefrLevel.B2, List.of("b2"));
    when(discordOAuthService.exchange(code)).thenReturn(profile);
    AccountEntity account = new AccountEntity();
    UUID accountId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    account.setId(accountId);
    account.setPersonId(personId);
    account.setStatus(AccountStatus.ACTIVE);
    when(gaiaAccountService.upsertProviderAccount(any(), any(), any(), any(), any())).thenReturn(account);
    UUID readerId = UUID.randomUUID();
    when(accountService.upsertFromDiscord(eq(accountId), eq("123"), any(), any(), any(), any(), any()))
        .thenReturn(readerId);
    when(jwtService.generateToken(accountId, personId)).thenReturn("jwt-token");

    DiscordLoginResult result = service.discordLogin(code, state);

    assertThat(result.getToken()).isEqualTo("jwt-token");
    assertThat(result.getAccountId()).isEqualTo(accountId.toString());
    assertThat(result.getReaderAccountId()).isEqualTo(readerId.toString());
    assertThat(result.getRequiresReactivation()).isFalse();
  }

  @Test
  void discordLogin_requiresReactivationWhenDeactivated() {
    String code = "code";
    DiscordOAuthService.DiscordProfile profile = new DiscordOAuthService.DiscordProfile(
        "123", "user", "User", "avatar", "user@example.com", null, List.of());
    when(discordOAuthService.exchange(code)).thenReturn(profile);
    AccountEntity account = new AccountEntity();
    UUID accountId = UUID.randomUUID();
    account.setId(accountId);
    account.setPersonId(UUID.randomUUID());
    account.setStatus(AccountStatus.DEACTIVATED);
    when(gaiaAccountService.upsertProviderAccount(any(), any(), any(), any(), any())).thenReturn(account);
    UUID readerId = UUID.randomUUID();
    when(accountService.upsertFromDiscord(eq(accountId), eq("123"), any(), any(), any(), any(), any()))
        .thenReturn(readerId);

    DiscordLoginResult result = service.discordLogin(code, "state");

    assertThat(result.getToken()).isEmpty();
    assertThat(result.getRequiresReactivation()).isTrue();
    assertThat(result.getAccountId()).isEqualTo(accountId.toString());
  }
}

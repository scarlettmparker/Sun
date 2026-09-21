package com.sun.gaia.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.base.error.MutationException;
import com.sun.fates.model.PersonEntity;
import com.sun.fates.service.PersonService;
import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.codegen.types.AuthResult;
import com.sun.gaia.codegen.types.ChangePasswordResponse;
import com.sun.gaia.codegen.types.ConfirmAccountReactivationResponse;
import com.sun.gaia.codegen.types.DeactivateAccountResponse;
import com.sun.gaia.codegen.types.LoginInput;
import com.sun.gaia.codegen.types.RegisterInput;
import com.sun.gaia.codegen.types.RequestAccountReactivationResponse;
import com.sun.gaia.codegen.types.SuspendAccountResponse;
import com.sun.gaia.codegen.types.UnsuspendAccountResponse;
import com.sun.gaia.graphql.mappers.AccountMapper;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.ReactivationTokenEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.EmailService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.PasswordResetService;
import com.sun.gaia.service.ReactivationService;
import com.sun.gaia.service.UserContextHolder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountGraphQLServiceTest {

  @Mock private AccountService accountService;
  @Mock private AccountRepository accountRepository;
  @Mock private PersonService personService;
  @Mock private JwtService jwtService;
  @Mock private EmailService emailService;
  @Mock private PasswordResetService passwordResetService;
  @Mock private ReactivationService reactivationService;
  @Mock private AccountMapper accountMapper;

  @InjectMocks private AccountGraphQLService service;

  @AfterEach
  void clearContext() {
    UserContextHolder.clear();
  }

  @Test
  void login_returnsAuthResultWhenCredentialsValid() {
    UUID accountId = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(accountId);
    account.setPersonId(personId);

    when(accountService.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(accountService.verifyPassword(account, "pass123")).thenReturn(true);
    when(jwtService.generateToken(accountId, personId)).thenReturn("jwt-token");

    AuthResult result = service.login(LoginInput.newBuilder()
        .username("testuser").password("pass123").build());

    assertThat(result.getToken()).isEqualTo("jwt-token");
    assertThat(result.getAccountId()).isEqualTo(accountId.toString());
    assertThat(result.getPersonId()).isEqualTo(personId.toString());
  }

  @Test
  void login_throwsWhenUserNotFound() {
    when(accountService.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.login(LoginInput.newBuilder()
        .username("unknown").password("pass").build()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void login_throwsWhenPasswordIncorrect() {
    AccountEntity account = new AccountEntity();
    account.setId(UUID.randomUUID());
    when(accountService.findByUsername("testuser")).thenReturn(Optional.of(account));
    when(accountService.verifyPassword(account, "wrong")).thenReturn(false);

    assertThatThrownBy(() -> service.login(LoginInput.newBuilder()
        .username("testuser").password("wrong").build()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void register_createsPersonAndAccount() {
    UUID personId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();
    PersonEntity person = new PersonEntity();
    person.setId(personId);
    when(personService.save(any(PersonEntity.class))).thenReturn(person);
    when(accountService.findByUsername("newuser")).thenReturn(Optional.empty());

    AccountEntity account = new AccountEntity();
    account.setId(accountId);
    account.setPersonId(personId);
    when(accountService.createAccount("newuser", "pass123", personId)).thenReturn(account);
    when(jwtService.generateToken(accountId, personId)).thenReturn("jwt-token");

    AuthResult result = service.register(RegisterInput.newBuilder()
        .username("newuser").password("pass123")
        .firstName("Jane").lastName("Doe").email("jane@test.com").build());

    assertThat(result.getAccountId()).isEqualTo(accountId.toString());
    assertThat(result.getPersonId()).isEqualTo(personId.toString());
    assertThat(result.getToken()).isEqualTo("jwt-token");
  }

  @Test
  void register_throwsWhenUsernameTaken() {
    when(accountService.findByUsername("taken")).thenReturn(Optional.of(new AccountEntity()));

    assertThatThrownBy(() -> service.register(RegisterInput.newBuilder()
        .username("taken").password("pass")
        .firstName("A").lastName("B").email("a@b.com").build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Username already taken");
  }

  @Test
  void changePassword_throwsWhenNotAuthenticated() {
    assertThatThrownBy(() -> service.changePassword("old", "new"))
        .isInstanceOf(MutationException.class)
        .hasMessage("Not authenticated");
  }

  @Test
  void changePassword_throwsWhenCurrentPasswordWrong() {
    UUID userId = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(userId);
    UserContextHolder.setUserId(userId);
    try {
      when(accountService.findById(userId)).thenReturn(Optional.of(account));
      when(accountService.verifyPassword(account, "wrong")).thenReturn(false);

      assertThatThrownBy(() -> service.changePassword("wrong", "new"))
          .isInstanceOf(MutationException.class)
          .hasMessage("Current password incorrect");
    } finally {
      UserContextHolder.clear();
    }
  }

  @Test
  void changePassword_succeedsWhenCurrentPasswordCorrect() {
    UUID userId = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(userId);
    UserContextHolder.setUserId(userId);
    when(accountService.findById(userId)).thenReturn(Optional.of(account));
    when(accountService.verifyPassword(account, "old")).thenReturn(true);

    ChangePasswordResponse result = service.changePassword("old", "new");

    assertThat(result.getMessage()).isEqualTo("Password changed");
    verify(accountService).changePassword(userId, "new");
  }

  @Test
  void suspendAccount_marksAccountSuspended() {
    UUID id = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(id);
    account.setStatus(AccountStatus.ACTIVE);
    Account mapped = Account.newBuilder().id(id.toString()).status(AccountStatus.SUSPENDED).build();
    when(accountService.findById(id)).thenReturn(Optional.of(account));
    when(accountMapper.map(account)).thenReturn(mapped);

    SuspendAccountResponse result = service.suspendAccount(id.toString());

    assertThat(result.getMessage()).isEqualTo("Account suspended");
    assertThat(result.getAccount()).isEqualTo(mapped);
    assertThat(account.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
    verify(accountService).save(account);
  }

  @Test
  void suspendAccount_throwsWhenAccountNotFound() {
    UUID id = UUID.randomUUID();
    when(accountService.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.suspendAccount(id.toString()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void unsuspendAccount_reactivatesAccount() {
    UUID id = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(id);
    account.setStatus(AccountStatus.SUSPENDED);
    Account mapped = Account.newBuilder().id(id.toString()).status(AccountStatus.ACTIVE).build();
    when(accountService.findById(id)).thenReturn(Optional.of(account));
    when(accountMapper.map(account)).thenReturn(mapped);

    UnsuspendAccountResponse result = service.unsuspendAccount(id.toString());

    assertThat(result.getAccount()).isEqualTo(mapped);
    assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    verify(accountService).save(account);
  }

  @Test
  void deactivateAccount_marksAccountDeactivated() {
    UUID userId = UUID.randomUUID();
    UserContextHolder.setUserId(userId);
    try {
      AccountEntity account = new AccountEntity();
      account.setId(userId);
      when(accountService.deactivateAccount(userId)).thenReturn(account);

      DeactivateAccountResponse result = service.deactivateAccount();

      assertThat(result.getMessage()).isEqualTo("Account deactivated");
      verify(accountService).deactivateAccount(userId);
    } finally {
      UserContextHolder.clear();
    }
  }

  @Test
  void deactivateAccount_throwsWhenNotAuthenticated() {
    assertThatThrownBy(() -> service.deactivateAccount())
        .isInstanceOf(MutationException.class)
        .hasMessage("Not authenticated");
  }

  @Test
  void requestAccountReactivation_sendsEmailForDeactivatedAccount() {
    UUID accountId = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(accountId);
    account.setProvider("discord");
    account.setStatus(AccountStatus.DEACTIVATED);
    when(accountService.findByPersonEmail("user@test.com")).thenReturn(List.of(account));
    ReactivationTokenEntity token = new ReactivationTokenEntity();
    token.setToken("reactivation-token");
    when(reactivationService.createToken(accountId)).thenReturn(token);

    RequestAccountReactivationResponse result =
        service.requestAccountReactivation("user@test.com", "discord");

    assertThat(result.getMessage()).isEqualTo("Reactivation email sent");
    verify(emailService).sendReactivationEmail(eq("user@test.com"), contains("reactivation-token"));
  }

  @Test
  void requestAccountReactivation_doesNotEmailActiveAccount() {
    AccountEntity account = new AccountEntity();
    account.setProvider("discord");
    account.setStatus(AccountStatus.ACTIVE);
    when(accountService.findByPersonEmail("active@test.com")).thenReturn(List.of(account));

    RequestAccountReactivationResponse result =
        service.requestAccountReactivation("active@test.com", "discord");

    assertThat(result.getMessage()).isEqualTo("Reactivation email sent");
    verify(emailService, never()).sendReactivationEmail(anyString(), anyString());
    verify(reactivationService, never()).createToken(any());
  }

  @Test
  void requestAccountReactivation_picksDeactivatedDiscordAccount() {
    AccountEntity local = new AccountEntity();
    local.setId(UUID.randomUUID());
    local.setProvider("local");
    local.setStatus(AccountStatus.DEACTIVATED);
    UUID discordId = UUID.randomUUID();
    AccountEntity discord = new AccountEntity();
    discord.setId(discordId);
    discord.setProvider("discord");
    discord.setStatus(AccountStatus.DEACTIVATED);
    when(accountService.findByPersonEmail("shared@test.com"))
        .thenReturn(List.of(local, discord));
    ReactivationTokenEntity token = new ReactivationTokenEntity();
    token.setToken("reactivation-token");
    when(reactivationService.createToken(discordId)).thenReturn(token);

    RequestAccountReactivationResponse result =
        service.requestAccountReactivation("shared@test.com", "discord");

    assertThat(result.getMessage()).isEqualTo("Reactivation email sent");
    verify(reactivationService).createToken(discordId);
    verify(emailService).sendReactivationEmail(eq("shared@test.com"), contains("reactivation-token"));
  }

  @Test
  void requestAccountReactivation_ignoresNonMatchingProvider() {
    AccountEntity local = new AccountEntity();
    local.setId(UUID.randomUUID());
    local.setProvider("local");
    local.setStatus(AccountStatus.DEACTIVATED);
    when(accountService.findByPersonEmail("local@test.com")).thenReturn(List.of(local));

    RequestAccountReactivationResponse result =
        service.requestAccountReactivation("local@test.com", "discord");

    assertThat(result.getMessage()).isEqualTo("Reactivation email sent");
    verify(emailService, never()).sendReactivationEmail(anyString(), anyString());
    verify(reactivationService, never()).createToken(any());
  }

  @Test
  void confirmAccountReactivation_reactivatesAccount() {
    UUID accountId = UUID.randomUUID();
    when(reactivationService.useToken("reactivation-token")).thenReturn(accountId);
    AccountEntity account = new AccountEntity();
    account.setId(accountId);
    account.setStatus(AccountStatus.DEACTIVATED);
    when(accountService.findById(accountId)).thenReturn(Optional.of(account));

    ConfirmAccountReactivationResponse result =
        service.confirmAccountReactivation("reactivation-token");

    assertThat(result.getMessage()).isEqualTo("Account reactivated");
    assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    verify(accountService).save(account);
  }

  @Test
  void confirmAccountReactivation_throwsWhenTokenInvalid() {
    when(reactivationService.useToken("bad-token"))
        .thenThrow(new IllegalArgumentException("Invalid reactivation token"));

    assertThatThrownBy(() -> service.confirmAccountReactivation("bad-token"))
        .isInstanceOf(MutationException.class)
        .hasMessageContaining("Invalid reactivation token");
  }

  @Test
  void me_returnsNullWhenNotAuthenticated() {
    assertThat(service.me()).isNull();
  }

  @Test
  void me_returnsMappedAccountWhenAuthenticated() {
    UUID userId = UUID.randomUUID();
    UserContextHolder.setUserId(userId);
    AccountEntity entity = new AccountEntity();
    entity.setId(userId);
    Account mapped = Account.newBuilder().id(userId.toString()).username("test").build();
    when(accountService.findById(userId)).thenReturn(Optional.of(entity));
    when(accountMapper.map(entity)).thenReturn(mapped);

    Account result = service.me();

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void account_returnsNullWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(accountService.findById(id)).thenReturn(Optional.empty());

    assertThat(service.account(id.toString())).isNull();
  }

  @Test
  void account_returnsMappedWhenFound() {
    UUID id = UUID.randomUUID();
    AccountEntity entity = new AccountEntity();
    entity.setId(id);
    Account mapped = Account.newBuilder().id(id.toString()).username("found").build();
    when(accountService.findById(id)).thenReturn(Optional.of(entity));
    when(accountMapper.map(entity)).thenReturn(mapped);

    assertThat(service.account(id.toString())).isEqualTo(mapped);
  }
}

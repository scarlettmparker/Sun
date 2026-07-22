package com.sun.gaia.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.fates.model.PersonEntity;
import com.sun.fates.service.PersonService;
import com.sun.gaia.codegen.types.AuthResult;
import com.sun.gaia.codegen.types.LoginInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.RegisterInput;
import com.sun.gaia.codegen.types.StandardError;
import com.sun.gaia.graphql.mappers.AccountMapper;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.AccountService;
import com.sun.gaia.service.EmailService;
import com.sun.gaia.service.JwtService;
import com.sun.gaia.service.PasswordResetService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GaiaGraphQLServiceTest {

  @Mock private AccountService accountService;
  @Mock private AccountRepository accountRepository;
  @Mock private PersonService personService;
  @Mock private JwtService jwtService;
  @Mock private EmailService emailService;
  @Mock private PasswordResetService passwordResetService;
  @Mock private AccountMapper accountMapper;

  @InjectMocks private GaiaGraphQLService service;

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
  void changePassword_returnsErrorWhenNotAuthenticated() {
    QueryResult result = service.changePassword("old", "new");

    assertThat(result).isInstanceOf(StandardError.class);
    assertThat(((StandardError) result).getMessage()).isEqualTo("Not authenticated");
  }

  @Test
  void changePassword_returnsErrorWhenCurrentPasswordWrong() {
    UUID userId = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(userId);
    com.sun.gaia.service.UserContextHolder.setUserId(userId);
    try {
      when(accountService.findById(userId)).thenReturn(Optional.of(account));
      when(accountService.verifyPassword(account, "wrong")).thenReturn(false);

      QueryResult result = service.changePassword("wrong", "new");

      assertThat(result).isInstanceOf(StandardError.class);
      assertThat(((StandardError) result).getMessage()).isEqualTo("Current password incorrect");
    } finally {
      com.sun.gaia.service.UserContextHolder.clear();
    }
  }

  @Test
  void myRoles_returnsRoleKeysWhenAuthenticated() {
    UUID userId = UUID.randomUUID();
    com.sun.gaia.service.UserContextHolder.setUserId(userId);
    try {
      when(accountRepository.findEffectiveRoleNames(userId)).thenReturn(List.of("admin"));

      List<String> roles = service.myRoles();

      assertThat(roles).containsExactly("admin");
    } finally {
      com.sun.gaia.service.UserContextHolder.clear();
    }
  }

  @Test
  void myRoles_returnsEmptyListWhenNotAuthenticated() {
    assertThat(service.myRoles()).isEmpty();
  }

  @Test
  void suspendAccount_marksAccountSuspended() {
    UUID id = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(id);
    account.setStatus(AccountStatus.ACTIVE);
    when(accountService.findById(id)).thenReturn(Optional.of(account));

    QueryResult result = service.suspendAccount(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
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
    when(accountService.findById(id)).thenReturn(Optional.of(account));

    QueryResult result = service.unsuspendAccount(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    verify(accountService).save(account);
  }
}

package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.codegen.types.AuthResult;
import com.sun.gaia.codegen.types.LoginInput;
import com.sun.gaia.codegen.types.PagedAccounts;
import com.sun.gaia.codegen.types.PaginationInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.RegisterInput;
import com.sun.gaia.graphql.services.AccountGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountDataFetcherTest {

  @Mock private AccountGraphQLService service;

  @InjectMocks private AccountDataFetcher fetcher;

  @Test
  void me_shouldDelegateToService() {
    Account mapped = Account.newBuilder().id("id").username("test").build();
    when(service.me()).thenReturn(mapped);

    Account result = fetcher.me();

    assertThat(result).isEqualTo(mapped);
    verify(service).me();
  }

  @Test
  void account_shouldDelegateToService() {
    Account mapped = Account.newBuilder().id("id").username("test").build();
    when(service.account("id")).thenReturn(mapped);

    Account result = fetcher.account("id");

    assertThat(result).isEqualTo(mapped);
    verify(service).account("id");
  }

  @Test
  void listAccounts_shouldDelegateToService() {
    Account a = Account.newBuilder().id("id").username("test").build();
    when(service.listAccounts()).thenReturn(List.of(a));

    List<Account> result = fetcher.listAccounts();

    assertThat(result).containsExactly(a);
    verify(service).listAccounts();
  }

  @Test
  void accounts_shouldDelegateToService() {
    PagedAccounts page = PagedAccounts.newBuilder().items(List.of()).build();
    PaginationInput pagination = PaginationInput.newBuilder().page(0).size(10).build();
    when(service.accounts(pagination)).thenReturn(page);

    PagedAccounts result = fetcher.accounts(pagination);

    assertThat(result).isEqualTo(page);
    verify(service).accounts(pagination);
  }

  @Test
  void suspendAccount_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("Account suspended").id("id").build();
    when(service.suspendAccount("id")).thenReturn(mockResult);

    QueryResult result = fetcher.suspendAccount("id");

    assertThat(result).isEqualTo(mockResult);
    verify(service).suspendAccount("id");
  }

  @Test
  void unsuspendAccount_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("Account unsuspended").id("id").build();
    when(service.unsuspendAccount("id")).thenReturn(mockResult);

    QueryResult result = fetcher.unsuspendAccount("id");

    assertThat(result).isEqualTo(mockResult);
    verify(service).unsuspendAccount("id");
  }

  @Test
  void deactivateAccount_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("Account deactivated").id("id").build();
    when(service.deactivateAccount()).thenReturn(mockResult);

    QueryResult result = fetcher.deactivateAccount();

    assertThat(result).isEqualTo(mockResult);
    verify(service).deactivateAccount();
  }

  @Test
  void register_shouldDelegateToService() {
    RegisterInput input = RegisterInput.newBuilder().username("newuser").password("pass")
        .firstName("A").lastName("B").email("a@b.com").build();
    AuthResult mockResult = AuthResult.newBuilder().accountId("id").personId("pid").token("jwt").build();
    when(service.register(input)).thenReturn(mockResult);

    AuthResult result = fetcher.register(input);

    assertThat(result).isEqualTo(mockResult);
    verify(service).register(input);
  }

  @Test
  void login_shouldDelegateToService() {
    LoginInput input = LoginInput.newBuilder().username("user").password("pass").build();
    AuthResult mockResult = AuthResult.newBuilder().accountId("id").personId("pid").token("jwt").build();
    when(service.login(input)).thenReturn(mockResult);

    AuthResult result = fetcher.login(input);

    assertThat(result).isEqualTo(mockResult);
    verify(service).login(input);
  }

  @Test
  void logout_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("Logout succeeded").build();
    when(service.logout()).thenReturn(mockResult);

    QueryResult result = fetcher.logout();

    assertThat(result).isEqualTo(mockResult);
    verify(service).logout();
  }

  @Test
  void requestPasswordReset_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("Password reset email sent").build();
    when(service.requestPasswordReset("a@b.com")).thenReturn(mockResult);

    QueryResult result = fetcher.requestPasswordReset("a@b.com");

    assertThat(result).isEqualTo(mockResult);
    verify(service).requestPasswordReset("a@b.com");
  }

  @Test
  void resetPassword_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("Password reset succeeded").id("id").build();
    when(service.resetPassword("token", "newPass")).thenReturn(mockResult);

    QueryResult result = fetcher.resetPassword("token", "newPass");

    assertThat(result).isEqualTo(mockResult);
    verify(service).resetPassword("token", "newPass");
  }

  @Test
  void changePassword_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("Password changed").id("id").build();
    when(service.changePassword("old", "new")).thenReturn(mockResult);

    QueryResult result = fetcher.changePassword("old", "new");

    assertThat(result).isEqualTo(mockResult);
    verify(service).changePassword("old", "new");
  }

  @Test
  void requestAccountReactivation_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("Reactivation email sent").build();
    when(service.requestAccountReactivation("a@b.com", "discord")).thenReturn(mockResult);

    QueryResult result = fetcher.requestAccountReactivation("a@b.com", "discord");

    assertThat(result).isEqualTo(mockResult);
    verify(service).requestAccountReactivation("a@b.com", "discord");
  }

  @Test
  void confirmAccountReactivation_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("Account reactivated").id("id").build();
    when(service.confirmAccountReactivation("token")).thenReturn(mockResult);

    QueryResult result = fetcher.confirmAccountReactivation("token");

    assertThat(result).isEqualTo(mockResult);
    verify(service).confirmAccountReactivation("token");
  }
}

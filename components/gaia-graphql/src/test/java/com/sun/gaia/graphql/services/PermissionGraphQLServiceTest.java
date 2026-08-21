package com.sun.gaia.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountStatus;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.service.RoleAdminService;
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
class PermissionGraphQLServiceTest {

  @Mock private AccountRepository accountRepository;
  @Mock private RoleAdminService roleAdminService;

  @InjectMocks private PermissionGraphQLService service;

  @AfterEach
  void clearContext() {
    UserContextHolder.clear();
  }

  @Test
  void myRoles_returnsRoleKeysWhenAuthenticated() {
    UUID userId = UUID.randomUUID();
    UserContextHolder.setUserId(userId);
    try {
      when(accountRepository.findEffectiveRoleNames(userId)).thenReturn(List.of("admin"));

      List<String> roles = service.myRoles();

      assertThat(roles).containsExactly("admin");
    } finally {
      UserContextHolder.clear();
    }
  }

  @Test
  void myRoles_returnsEmptyListWhenNotAuthenticated() {
    assertThat(service.myRoles()).isEmpty();
  }

  @Test
  void effectivePermissions_returnsPatternsForActiveDiscordAccount() {
    UUID accountId = UUID.randomUUID();
    AccountEntity account = new AccountEntity();
    account.setId(accountId);
    when(accountRepository.findByProviderAndProviderIdAndStatus(
        "discord", "12345", AccountStatus.ACTIVE))
        .thenReturn(Optional.of(account));
    when(accountRepository.findEffectivePermissions(accountId))
        .thenReturn(List.of("bot.commands.*"));

    List<String> result = service.effectivePermissions(RemoteUserType.DISCORD, "12345");

    assertThat(result).containsExactly("bot.commands.*");
  }

  @Test
  void effectivePermissions_returnsEmptyForUnknownUser() {
    when(accountRepository.findByProviderAndProviderIdAndStatus(
        "discord", "unknown", AccountStatus.ACTIVE))
        .thenReturn(Optional.empty());

    List<String> result = service.effectivePermissions(RemoteUserType.DISCORD, "unknown");

    assertThat(result).isEmpty();
  }

  @Test
  void effectivePermissions_returnsEmptyForBlankUserId() {
    List<String> result = service.effectivePermissions(RemoteUserType.DISCORD, "  ");

    assertThat(result).isEmpty();
  }

  @Test
  void effectivePermissions_returnsEmptyWhenUserIdNull() {
    List<String> result = service.effectivePermissions(RemoteUserType.DISCORD, null);

    assertThat(result).isEmpty();
  }

  @Test
  void accountPermissions_delegatesToAdminService() {
    UUID accountId = UUID.randomUUID();
    when(roleAdminService.accountPermissions(accountId)).thenReturn(List.of("graphql.gaia.me"));

    List<String> result = service.accountPermissions(accountId.toString());

    assertThat(result).containsExactly("graphql.gaia.me");
    verify(roleAdminService).accountPermissions(accountId);
  }

  @Test
  void rolePermissions_delegatesToAdminService() {
    UUID roleId = UUID.randomUUID();
    when(roleAdminService.rolePermissions(roleId)).thenReturn(List.of("graphql.gaia.roles"));

    List<String> result = service.rolePermissions(roleId.toString());

    assertThat(result).containsExactly("graphql.gaia.roles");
    verify(roleAdminService).rolePermissions(roleId);
  }

  @Test
  void setAccountPermissions_delegatesAndReturnsSuccess() {
    UUID accountId = UUID.randomUUID();
    List<String> perms = List.of("graphql.gaia.me");

    QueryResult result = service.setAccountPermissions(accountId.toString(), perms);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(accountId.toString());
    verify(roleAdminService).setAccountPermissions(accountId, perms);
  }

  @Test
  void setRolePermissions_delegatesAndReturnsSuccess() {
    UUID roleId = UUID.randomUUID();
    List<String> perms = List.of("graphql.gaia.roles");

    QueryResult result = service.setRolePermissions(roleId.toString(), perms);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(roleId.toString());
    verify(roleAdminService).setRolePermissions(roleId, perms);
  }

  @Test
  void setAccountPermissions_handlesEmptyList() {
    UUID accountId = UUID.randomUUID();
    List<String> perms = List.of();

    QueryResult result = service.setAccountPermissions(accountId.toString(), perms);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    verify(roleAdminService).setAccountPermissions(accountId, perms);
  }

  @Test
  void setRolePermissions_handlesEmptyList() {
    UUID roleId = UUID.randomUUID();
    List<String> perms = List.of();

    QueryResult result = service.setRolePermissions(roleId.toString(), perms);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    verify(roleAdminService).setRolePermissions(roleId, perms);
  }
}

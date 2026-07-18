package com.sun.gaia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sun.gaia.repository.AccountRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

  @Mock private AccountRepository accountRepository;

  @InjectMocks private PermissionService permissions;

  private final UUID accountId = UUID.randomUUID();

  @AfterEach
  void clearContext() {
    UserContextHolder.clear();
  }

  @Test
  void match_shouldHandleWildcardSuffix() {
    assertThat(PermissionService.match("graphql.icarus.thread", "graphql.icarus.*")).isTrue();
  }

  @Test
  void match_shouldHandleModuleWildcard() {
    assertThat(PermissionService.match("graphql.icarus.thread", "graphql.*")).isTrue();
  }

  @Test
  void match_shouldHandleGlobalWildcard() {
    assertThat(PermissionService.match("graphql.icarus.thread", "*")).isTrue();
  }

  @Test
  void match_shouldAcceptExactPermission() {
    assertThat(PermissionService.match("graphql.icarus.thread", "graphql.icarus.thread"))
        .isTrue();
  }

  @Test
  void match_shouldNotMatchUnrelatedWildcard() {
    assertThat(PermissionService.match("graphql.icarus.thread", "graphql.hades.*")).isFalse();
  }

  @Test
  void match_shouldTreatDotAsLiteral() {
    assertThat(PermissionService.match("graphql.icarus.threads", "graphql.icarus.thread"))
        .isFalse();
  }

  @Test
  void has_shouldAggregateDirectAndRolePermissions() {
    UserContextHolder.setUserId(accountId);
    when(accountRepository.findEffectivePermissions(accountId))
        .thenReturn(List.of("graphql.icarus.*", "graphql.hades.annotations"));

    assertThat(permissions.has("graphql.icarus.thread")).isTrue();
    assertThat(permissions.has("graphql.hades.annotations")).isTrue();
    assertThat(permissions.has("graphql.gaia.me")).isFalse();
  }

  @Test
  void has_shouldDenyAnonymous() {
    assertThat(permissions.has("graphql.icarus.thread")).isFalse();
  }

  @Test
  void has_shouldDenyAccountWithNoPermissions() {
    UserContextHolder.setUserId(accountId);
    when(accountRepository.findEffectivePermissions(accountId)).thenReturn(List.of());

    assertThat(permissions.has("graphql.icarus.thread")).isFalse();
  }

  @Test
  void require_shouldThrowWhenDenied() {
    UserContextHolder.setUserId(accountId);
    when(accountRepository.findEffectivePermissions(accountId)).thenReturn(List.of());

    assertThatThrownBy(() -> permissions.require("graphql.icarus.thread"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void isAuthenticated_shouldReflectContext() {
    assertThat(permissions.isAuthenticated()).isFalse();
    UserContextHolder.setUserId(accountId);
    assertThat(permissions.isAuthenticated()).isTrue();
  }
}

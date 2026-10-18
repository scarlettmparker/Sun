package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.CreateRoleResponse;
import com.sun.gaia.codegen.types.DeleteRoleResponse;
import com.sun.gaia.codegen.types.Role;
import com.sun.gaia.codegen.types.SetAccountRolesResponse;
import com.sun.gaia.graphql.mappers.RoleMapper;
import com.sun.gaia.model.RoleEntity;
import com.sun.gaia.service.RoleAdminService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleDataFetcherTest {

  @Mock private RoleAdminService roleAdminService;
  @Mock private RoleMapper roleMapper;

  @InjectMocks private RoleDataFetcher fetcher;

  @Test
  void roles_returnsMappedList() {
    RoleEntity entity = new RoleEntity();
    entity.setName("admin");
    Role mapped = Role.newBuilder().name("admin").build();
    when(roleAdminService.listRoles()).thenReturn(List.of(entity));
    when(roleMapper.map(entity)).thenReturn(mapped);

    List<Role> result = fetcher.roles();

    assertThat(result).containsExactly(mapped);
    verify(roleAdminService).listRoles();
  }

  @Test
  void roles_returnsEmptyWhenNoRoles() {
    when(roleAdminService.listRoles()).thenReturn(List.of());

    assertThat(fetcher.roles()).isEmpty();
  }

  @Test
  void role_returnsNullWhenAbsent() {
    UUID id = UUID.randomUUID();
    when(roleAdminService.findRoleById(id)).thenReturn(Optional.empty());

    assertThat(fetcher.role(id.toString())).isNull();
  }

  @Test
  void role_returnsMappedWhenPresent() {
    UUID id = UUID.randomUUID();
    RoleEntity entity = new RoleEntity();
    entity.setName("editor");
    Role mapped = Role.newBuilder().id(id.toString()).name("editor").build();
    when(roleAdminService.findRoleById(id)).thenReturn(Optional.of(entity));
    when(roleMapper.map(entity)).thenReturn(mapped);

    assertThat(fetcher.role(id.toString())).isEqualTo(mapped);
  }

  @Test
  void accountRoles_delegatesToAdminService() {
    UUID accountId = UUID.randomUUID();
    when(roleAdminService.accountRoleNames(accountId)).thenReturn(List.of("admin", "editor"));

    List<String> result = fetcher.accountRoles(accountId.toString());

    assertThat(result).containsExactly("admin", "editor");
    verify(roleAdminService).accountRoleNames(accountId);
  }

  @Test
  void createRole_trimsAndDelegates() {
    RoleEntity entity = new RoleEntity();
    entity.setName("admin");
    Role mapped = Role.newBuilder().name("admin").build();
    when(roleAdminService.createRole("admin", "desc")).thenReturn(entity);
    when(roleMapper.map(entity)).thenReturn(mapped);

    CreateRoleResponse result = fetcher.createRole("admin", "desc");

    assertThat(result.getRole()).isEqualTo(mapped);
    verify(roleAdminService).createRole("admin", "desc");
  }

  @Test
  void createRole_delegatesWithDescription() {
    RoleEntity entity = new RoleEntity();
    entity.setName("viewer");
    Role mapped = Role.newBuilder().name("viewer").build();
    when(roleAdminService.createRole("viewer", null)).thenReturn(entity);
    when(roleMapper.map(entity)).thenReturn(mapped);

    CreateRoleResponse result = fetcher.createRole("viewer", null);

    assertThat(result.getRole()).isEqualTo(mapped);
  }

  @Test
  void deleteRole_delegatesAndReturnsSuccess() {
    UUID id = UUID.randomUUID();

    DeleteRoleResponse result = fetcher.deleteRole(id.toString());

    assertThat(result.getMessage()).isEqualTo("Role deleted");
    assertThat(result.getId()).isEqualTo(id.toString());
    verify(roleAdminService).deleteRole(id);
  }

  @Test
  void setAccountRoles_delegatesAndReturnsSuccess() {
    UUID accountId = UUID.randomUUID();
    List<String> roleNames = List.of("admin");

    SetAccountRolesResponse result = fetcher.setAccountRoles(accountId.toString(), roleNames);

    assertThat(result.getMessage()).isEqualTo("Account roles updated");
    assertThat(result.getAccountId()).isEqualTo(accountId.toString());
    assertThat(result.getRoles()).containsExactly("admin");
    verify(roleAdminService).setAccountRoles(accountId, roleNames);
  }

  @Test
  void setAccountRoles_validatesUnknownRole() {
    UUID accountId = UUID.randomUUID();
    List<String> roleNames = List.of("unknown");
    when(roleAdminService.setAccountRoles(accountId, roleNames))
        .thenThrow(new IllegalArgumentException("Unknown role: unknown"));

    assertThatThrownBy(
        () -> fetcher.setAccountRoles(accountId.toString(), roleNames))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown role");
  }
}

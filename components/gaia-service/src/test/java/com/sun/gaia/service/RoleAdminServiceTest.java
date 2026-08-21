package com.sun.gaia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.model.AccountPermissionEntity;
import com.sun.gaia.model.AccountRoleEntity;
import com.sun.gaia.model.RoleEntity;
import com.sun.gaia.model.RolePermissionEntity;
import com.sun.gaia.repository.AccountPermissionRepository;
import com.sun.gaia.repository.AccountRepository;
import com.sun.gaia.repository.AccountRoleRepository;
import com.sun.gaia.repository.RolePermissionRepository;
import com.sun.gaia.repository.RoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleAdminServiceTest {

  @Mock private AccountRepository accountRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private AccountRoleRepository accountRoleRepository;
  @Mock private AccountPermissionRepository accountPermissionRepository;
  @Mock private RolePermissionRepository rolePermissionRepository;

  @InjectMocks private RoleAdminService service;

  @Test
  void listRoles_returnsSorted() {
    RoleEntity r1 = new RoleEntity();
    r1.setName("admin");
    RoleEntity r2 = new RoleEntity();
    r2.setName("viewer");
    when(roleRepository.findAllByOrderByNameAsc()).thenReturn(List.of(r1, r2));

    List<RoleEntity> result = service.listRoles();

    assertThat(result).containsExactly(r1, r2);
    verify(roleRepository).findAllByOrderByNameAsc();
  }

  @Test
  void createRole_createsWhenNotExists() {
    when(roleRepository.findByName("admin")).thenReturn(Optional.empty());
    RoleEntity saved = new RoleEntity();
    saved.setName("admin");
    when(roleRepository.save(any(RoleEntity.class))).thenReturn(saved);

    RoleEntity result = service.createRole("admin", "desc");

    assertThat(result.getName()).isEqualTo("admin");
    verify(roleRepository).save(any(RoleEntity.class));
  }

  @Test
  void createRole_throwsWhenExists() {
    RoleEntity existing = new RoleEntity();
    existing.setName("admin");
    when(roleRepository.findByName("admin")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.createRole("admin", "desc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Role already exists");
  }

  @Test
  void createRole_throwsWhenBlank() {
    assertThatThrownBy(() -> service.createRole("  ", "desc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Role name is required");

    assertThatThrownBy(() -> service.createRole(null, "desc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Role name is required");
  }

  @Test
  void deleteRole_deletesPermissionsAndRoles() {
    UUID roleId = UUID.randomUUID();
    RoleEntity role = new RoleEntity();
    role.setId(roleId);
    role.setName("admin");
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

    service.deleteRole(roleId);

    verify(rolePermissionRepository).deleteByRoleId(roleId);
    verify(accountRoleRepository).deleteByRoleId(roleId);
    verify(roleRepository).delete(role);
  }

  @Test
  void deleteRole_throwsWhenNotFound() {
    UUID roleId = UUID.randomUUID();
    when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteRole(roleId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Role not found");
  }

  @Test
  void setAccountRoles_replacesRoles() {
    UUID accountId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();
    RoleEntity role = new RoleEntity();
    role.setId(roleId);
    role.setName("admin");

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(new com.sun.gaia.model.AccountEntity()));
    when(roleRepository.findByName("admin")).thenReturn(Optional.of(role));
    AccountRoleEntity link = new AccountRoleEntity();
    link.setAccountId(accountId);
    link.setRoleId(roleId);
    when(accountRoleRepository.findByAccountId(accountId)).thenReturn(List.of(link));
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

    List<String> result = service.setAccountRoles(accountId, List.of("admin"));

    verify(accountRoleRepository).deleteByAccountId(accountId);
    verify(accountRoleRepository).save(any(AccountRoleEntity.class));
    assertThat(result).containsExactly("admin");
  }

  @Test
  void setAccountRoles_throwsWhenUnknownRole() {
    UUID accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(new com.sun.gaia.model.AccountEntity()));
    when(roleRepository.findByName("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.setAccountRoles(accountId, List.of("unknown")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown role");
  }

  @Test
  void setAccountRoles_clearsWhenEmpty() {
    UUID accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(new com.sun.gaia.model.AccountEntity()));
    when(accountRoleRepository.findByAccountId(accountId)).thenReturn(List.of());

    List<String> result = service.setAccountRoles(accountId, List.of());

    verify(accountRoleRepository).deleteByAccountId(accountId);
    assertThat(result).isEmpty();
  }

  @Test
  void setAccountPermissions_replaces() {
    UUID accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(new com.sun.gaia.model.AccountEntity()));
    AccountPermissionEntity saved = new AccountPermissionEntity();
    saved.setAccountId(accountId);
    saved.setPermission("perm.read");
    when(accountPermissionRepository.findByAccountId(accountId))
        .thenReturn(List.of(saved));

    List<String> result = service.setAccountPermissions(accountId, List.of("perm.read"));

    verify(accountPermissionRepository).deleteByAccountId(accountId);
    verify(accountPermissionRepository).save(any(AccountPermissionEntity.class));
    assertThat(result).containsExactly("perm.read");
  }

  @Test
  void setRolePermissions_replaces() {
    UUID roleId = UUID.randomUUID();
    RoleEntity role = new RoleEntity();
    role.setId(roleId);
    role.setName("admin");
    when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
    RolePermissionEntity saved = new RolePermissionEntity();
    saved.setRoleId(roleId);
    saved.setPermission("perm.read");
    when(rolePermissionRepository.findByRoleId(roleId)).thenReturn(List.of(saved));

    List<String> result = service.setRolePermissions(roleId, List.of("perm.read"));

    verify(rolePermissionRepository).deleteByRoleId(roleId);
    verify(rolePermissionRepository).save(any(RolePermissionEntity.class));
    assertThat(result).containsExactly("perm.read");
  }

  @Test
  void accountRoleNames_returnsSortedDirectRoles() {
    UUID accountId = UUID.randomUUID();
    UUID r1Id = UUID.randomUUID();
    UUID r2Id = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(new com.sun.gaia.model.AccountEntity()));
    AccountRoleEntity l1 = new AccountRoleEntity();
    l1.setAccountId(accountId);
    l1.setRoleId(r1Id);
    AccountRoleEntity l2 = new AccountRoleEntity();
    l2.setAccountId(accountId);
    l2.setRoleId(r2Id);
    when(accountRoleRepository.findByAccountId(accountId)).thenReturn(List.of(l2, l1));
    RoleEntity r1 = new RoleEntity();
    r1.setId(r1Id);
    r1.setName("admin");
    RoleEntity r2 = new RoleEntity();
    r2.setId(r2Id);
    r2.setName("viewer");
    when(roleRepository.findById(r1Id)).thenReturn(Optional.of(r1));
    when(roleRepository.findById(r2Id)).thenReturn(Optional.of(r2));

    List<String> result = service.accountRoleNames(accountId);

    assertThat(result).containsExactly("admin", "viewer");
  }

  @Test
  void accountPermissions_returnsSorted() {
    UUID accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.of(new com.sun.gaia.model.AccountEntity()));
    AccountPermissionEntity p1 = new AccountPermissionEntity();
    p1.setPermission("perm.z");
    AccountPermissionEntity p2 = new AccountPermissionEntity();
    p2.setPermission("perm.a");
    when(accountPermissionRepository.findByAccountId(accountId)).thenReturn(List.of(p1, p2));

    List<String> result = service.accountPermissions(accountId);

    assertThat(result).containsExactly("perm.a", "perm.z");
  }
}

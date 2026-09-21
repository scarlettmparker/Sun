package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.codegen.types.SetAccountPermissionsResponse;
import com.sun.gaia.codegen.types.SetRolePermissionsResponse;
import com.sun.gaia.graphql.services.PermissionGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionDataFetcherTest {

  @Mock private PermissionGraphQLService service;

  @InjectMocks private PermissionDataFetcher fetcher;

  @Test
  void myRoles_shouldDelegate() {
    when(service.myRoles()).thenReturn(List.of("admin"));

    List<String> result = fetcher.myRoles();

    assertThat(result).containsExactly("admin");
    verify(service).myRoles();
  }

  @Test
  void effectivePermissions_shouldDelegate() {
    when(service.effectivePermissions(RemoteUserType.DISCORD, "123")).thenReturn(List.of("perm.read"));

    List<String> result = fetcher.effectivePermissions(RemoteUserType.DISCORD, "123");

    assertThat(result).containsExactly("perm.read");
    verify(service).effectivePermissions(RemoteUserType.DISCORD, "123");
  }

  @Test
  void accountPermissions_shouldDelegate() {
    when(service.accountPermissions("acc1")).thenReturn(List.of("perm.write"));

    List<String> result = fetcher.accountPermissions("acc1");

    assertThat(result).containsExactly("perm.write");
    verify(service).accountPermissions("acc1");
  }

  @Test
  void rolePermissions_shouldDelegate() {
    when(service.rolePermissions("role1")).thenReturn(List.of("perm.exec"));

    List<String> result = fetcher.rolePermissions("role1");

    assertThat(result).containsExactly("perm.exec");
    verify(service).rolePermissions("role1");
  }

  @Test
  void setAccountPermissions_shouldDelegate() {
    SetAccountPermissionsResponse mock = SetAccountPermissionsResponse.newBuilder()
        .message("Account permissions updated")
        .accountId("acc1")
        .permissions(List.of("perm.read"))
        .build();
    when(service.setAccountPermissions("acc1", List.of("perm.read"))).thenReturn(mock);

    SetAccountPermissionsResponse result =
        fetcher.setAccountPermissions("acc1", List.of("perm.read"));

    assertThat(result).isEqualTo(mock);
    verify(service).setAccountPermissions("acc1", List.of("perm.read"));
  }

  @Test
  void setRolePermissions_shouldDelegate() {
    SetRolePermissionsResponse mock = SetRolePermissionsResponse.newBuilder()
        .message("Role permissions updated")
        .roleId("role1")
        .permissions(List.of("perm.read"))
        .build();
    when(service.setRolePermissions("role1", List.of("perm.read"))).thenReturn(mock);

    SetRolePermissionsResponse result = fetcher.setRolePermissions("role1", List.of("perm.read"));

    assertThat(result).isEqualTo(mock);
    verify(service).setRolePermissions("role1", List.of("perm.read"));
  }
}

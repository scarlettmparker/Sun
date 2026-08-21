package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.Role;
import com.sun.gaia.graphql.services.RoleGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleDataFetcherTest {

  @Mock private RoleGraphQLService service;

  @InjectMocks private RoleDataFetcher fetcher;

  @Test
  void roles_shouldDelegate() {
    Role r = Role.newBuilder().id("id1").name("admin").build();
    when(service.roles()).thenReturn(List.of(r));

    List<Role> result = fetcher.roles();

    assertThat(result).containsExactly(r);
    verify(service).roles();
  }

  @Test
  void role_shouldDelegate() {
    Role r = Role.newBuilder().id("id1").name("admin").build();
    when(service.role("id1")).thenReturn(r);

    Role result = fetcher.role("id1");

    assertThat(result).isEqualTo(r);
    verify(service).role("id1");
  }

  @Test
  void accountRoles_shouldDelegate() {
    when(service.accountRoles("acc1")).thenReturn(List.of("admin"));

    List<String> result = fetcher.accountRoles("acc1");

    assertThat(result).containsExactly("admin");
    verify(service).accountRoles("acc1");
  }

  @Test
  void createRole_shouldDelegate() {
    Role r = Role.newBuilder().id("id1").name("admin").build();
    when(service.createRole("admin", "desc")).thenReturn(r);

    Role result = fetcher.createRole("admin", "desc");

    assertThat(result).isEqualTo(r);
    verify(service).createRole("admin", "desc");
  }

  @Test
  void deleteRole_shouldDelegate() {
    QueryResult mock = QuerySuccess.newBuilder().message("Role deleted").id("id1").build();
    when(service.deleteRole("id1")).thenReturn(mock);

    QueryResult result = fetcher.deleteRole("id1");

    assertThat(result).isEqualTo(mock);
    verify(service).deleteRole("id1");
  }

  @Test
  void setAccountRoles_shouldDelegate() {
    QueryResult mock = QuerySuccess.newBuilder().message("Account roles updated").id("acc1").build();
    when(service.setAccountRoles("acc1", List.of("admin"))).thenReturn(mock);

    QueryResult result = fetcher.setAccountRoles("acc1", List.of("admin"));

    assertThat(result).isEqualTo(mock);
    verify(service).setAccountRoles("acc1", List.of("admin"));
  }
}

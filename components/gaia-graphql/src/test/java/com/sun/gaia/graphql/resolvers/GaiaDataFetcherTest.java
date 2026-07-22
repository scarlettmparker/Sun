package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.graphql.services.GaiaGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GaiaDataFetcherTest {

  @Mock
  private GaiaGraphQLService service;

  @InjectMocks
  private GaiaDataFetcher fetcher;

  @Test
  void myRoles_shouldDelegateToService() {
    when(service.myRoles()).thenReturn(List.of("admin"));

    List<String> result = fetcher.myRoles();

    assertThat(result).containsExactly("admin");
  }

  @Test
  void suspendAccount_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder()
        .message("Account suspended").id("id").build();
    when(service.suspendAccount("id")).thenReturn(mockResult);

    QueryResult result = fetcher.suspendAccount("id");

    assertThat(result).isEqualTo(mockResult);
  }

  @Test
  void unsuspendAccount_shouldDelegateToService() {
    QueryResult mockResult = QuerySuccess.newBuilder()
        .message("Account unsuspended").id("id").build();
    when(service.unsuspendAccount("id")).thenReturn(mockResult);

    QueryResult result = fetcher.unsuspendAccount("id");

    assertThat(result).isEqualTo(mockResult);
  }
}

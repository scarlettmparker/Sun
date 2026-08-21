package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.HubRegistry;
import com.sun.gaia.codegen.types.HubRegistryInput;
import com.sun.gaia.graphql.services.HubRegistryGraphQLService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HubRegistryDataFetcherTest {

  @Mock private HubRegistryGraphQLService service;

  @InjectMocks private HubRegistryDataFetcher fetcher;

  @Test
  void hubRegistry_shouldDelegate() {
    HubRegistry mock = HubRegistry.newBuilder().build();
    when(service.hubRegistry()).thenReturn(mock);

    HubRegistry result = fetcher.hubRegistry();

    assertThat(result).isEqualTo(mock);
    verify(service).hubRegistry();
  }

  @Test
  void saveRegistry_shouldDelegate() {
    HubRegistryInput input = HubRegistryInput.newBuilder().build();
    HubRegistry mock = HubRegistry.newBuilder().build();
    when(service.saveRegistry(input)).thenReturn(mock);

    HubRegistry result = fetcher.saveRegistry(input);

    assertThat(result).isEqualTo(mock);
    verify(service).saveRegistry(input);
  }
}

package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.TailscaleDevice;
import com.sun.gaia.graphql.services.TailscaleGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TailscaleDataFetcherTest {

  @Mock private TailscaleGraphQLService service;

  @InjectMocks private TailscaleDataFetcher fetcher;

  @Test
  void tailscaleDevices_shouldDelegate() {
    TailscaleDevice d = TailscaleDevice.newBuilder().id("id1").name("node1").build();
    when(service.tailscaleDevices()).thenReturn(List.of(d));

    List<TailscaleDevice> result = fetcher.tailscaleDevices();

    assertThat(result).containsExactly(d);
    verify(service).tailscaleDevices();
  }

  @Test
  void tailscaleDevice_shouldDelegate() {
    TailscaleDevice d = TailscaleDevice.newBuilder().id("id1").name("node1").build();
    when(service.tailscaleDevice("id1")).thenReturn(d);

    TailscaleDevice result = fetcher.tailscaleDevice("id1");

    assertThat(result).isEqualTo(d);
    verify(service).tailscaleDevice("id1");
  }

  @Test
  void expireTailscaleDevice_shouldDelegate() {
    QueryResult mock = QuerySuccess.newBuilder().message("Tailscale device expired").id("id1").build();
    when(service.expireTailscaleDevice("id1")).thenReturn(mock);

    QueryResult result = fetcher.expireTailscaleDevice("id1");

    assertThat(result).isEqualTo(mock);
    verify(service).expireTailscaleDevice("id1");
  }
}

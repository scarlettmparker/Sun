package com.sun.gaia.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.codegen.types.TailscaleDevice;
import com.sun.gaia.graphql.mappers.TailscaleDeviceMapper;
import com.sun.gaia.model.TailscaleDeviceEntity;
import com.sun.gaia.service.TailscaleDeviceService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TailscaleGraphQLServiceTest {

  @Mock private TailscaleDeviceService tailscaleDeviceService;
  @Mock private TailscaleDeviceMapper tailscaleDeviceMapper;

  @InjectMocks private TailscaleGraphQLService service;

  @Test
  void tailscaleDevices_returnsMappedList() {
    TailscaleDeviceEntity entity = new TailscaleDeviceEntity();
    TailscaleDevice mapped = TailscaleDevice.newBuilder().name("device-1").build();
    when(tailscaleDeviceService.listAll()).thenReturn(List.of(entity));
    when(tailscaleDeviceMapper.map(List.of(entity))).thenReturn(List.of(mapped));

    List<TailscaleDevice> result = service.tailscaleDevices();

    assertThat(result).containsExactly(mapped);
    verify(tailscaleDeviceService).listAll();
  }

  @Test
  void tailscaleDevices_returnsEmptyWhenNone() {
    when(tailscaleDeviceService.listAll()).thenReturn(List.of());
    when(tailscaleDeviceMapper.map(List.of())).thenReturn(List.of());

    assertThat(service.tailscaleDevices()).isEmpty();
  }

  @Test
  void tailscaleDevice_returnsMappedWhenFound() {
    UUID id = UUID.randomUUID();
    TailscaleDeviceEntity entity = new TailscaleDeviceEntity();
    TailscaleDevice mapped = TailscaleDevice.newBuilder().id(id.toString()).build();
    when(tailscaleDeviceService.findById(id)).thenReturn(entity);
    when(tailscaleDeviceMapper.map(entity)).thenReturn(mapped);

    TailscaleDevice result = service.tailscaleDevice(id.toString());

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void tailscaleDevice_returnsNullWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(tailscaleDeviceService.findById(id)).thenThrow(new IllegalArgumentException("not found"));

    assertThat(service.tailscaleDevice(id.toString())).isNull();
  }

  @Test
  void expireTailscaleDevice_returnsSuccess() {
    UUID id = UUID.randomUUID();

    QueryResult result = service.expireTailscaleDevice(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    verify(tailscaleDeviceService).markExpired(id);
  }
}

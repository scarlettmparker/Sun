package com.sun.gaia.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.CreateIpWhitelistEntryResponse;
import com.sun.gaia.codegen.types.DeleteIpWhitelistEntryResponse;
import com.sun.gaia.codegen.types.IpWhitelistEntry;
import com.sun.gaia.codegen.types.IpWhitelistEntryInput;
import com.sun.gaia.codegen.types.UpdateIpWhitelistEntryResponse;
import com.sun.gaia.graphql.mappers.IpWhitelistMapper;
import com.sun.gaia.model.IpWhitelistEntryEntity;
import com.sun.gaia.service.IpWhitelistService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpWhitelistGraphQLServiceTest {

  @Mock private IpWhitelistService ipWhitelistService;
  @Mock private IpWhitelistMapper ipWhitelistMapper;

  @InjectMocks private IpWhitelistGraphQLService service;

  @Test
  void ipWhitelistEntries_returnsMappedList() {
    IpWhitelistEntryEntity entity = new IpWhitelistEntryEntity();
    entity.setPattern("192.168.0.0/24");
    IpWhitelistEntry mapped = IpWhitelistEntry.newBuilder().pattern("192.168.0.0/24").build();
    when(ipWhitelistService.listAll()).thenReturn(List.of(entity));
    when(ipWhitelistMapper.map(List.of(entity))).thenReturn(List.of(mapped));

    List<IpWhitelistEntry> result = service.ipWhitelistEntries();

    assertThat(result).containsExactly(mapped);
    verify(ipWhitelistService).listAll();
  }

  @Test
  void ipWhitelistEntries_returnsEmptyWhenNone() {
    when(ipWhitelistService.listAll()).thenReturn(List.of());
    when(ipWhitelistMapper.map(List.of())).thenReturn(List.of());

    assertThat(service.ipWhitelistEntries()).isEmpty();
  }

  @Test
  void createIpWhitelistEntry_returnsSuccessWithId() {
    IpWhitelistEntryInput input = IpWhitelistEntryInput.newBuilder()
        .pattern("10.0.0.1").description("test").immutable(false).build();
    IpWhitelistEntryEntity entity = new IpWhitelistEntryEntity();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    IpWhitelistEntry mapped = IpWhitelistEntry.newBuilder().id(id.toString()).pattern("10.0.0.1").build();
    when(ipWhitelistService.addEntry("10.0.0.1", "test", false)).thenReturn(entity);
    when(ipWhitelistMapper.map(entity)).thenReturn(mapped);

    CreateIpWhitelistEntryResponse result = service.createIpWhitelistEntry(input);

    assertThat(result.getMessage()).isEqualTo("IP whitelist entry created");
    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(ipWhitelistService).addEntry("10.0.0.1", "test", false);
  }

  @Test
  void createIpWhitelistEntry_handlesImmutableTrue() {
    IpWhitelistEntryInput input = IpWhitelistEntryInput.newBuilder()
        .pattern("10.0.0.1").immutable(true).build();
    IpWhitelistEntryEntity entity = new IpWhitelistEntryEntity();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    IpWhitelistEntry mapped = IpWhitelistEntry.newBuilder().id(id.toString()).pattern("10.0.0.1").build();
    when(ipWhitelistService.addEntry("10.0.0.1", null, true)).thenReturn(entity);
    when(ipWhitelistMapper.map(entity)).thenReturn(mapped);

    CreateIpWhitelistEntryResponse result = service.createIpWhitelistEntry(input);

    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(ipWhitelistService).addEntry("10.0.0.1", null, true);
  }

  @Test
  void updateIpWhitelistEntry_returnsResponse() {
    UUID id = UUID.randomUUID();
    IpWhitelistEntryInput input = IpWhitelistEntryInput.newBuilder()
        .pattern("10.0.0.2").description("updated").enabled(true).build();
    IpWhitelistEntryEntity entity = new IpWhitelistEntryEntity();
    entity.setId(id);
    IpWhitelistEntry mapped = IpWhitelistEntry.newBuilder().id(id.toString()).pattern("10.0.0.2").build();
    when(ipWhitelistService.updateEntry(id, "10.0.0.2", "updated", true)).thenReturn(entity);
    when(ipWhitelistMapper.map(entity)).thenReturn(mapped);

    UpdateIpWhitelistEntryResponse result = service.updateIpWhitelistEntry(id.toString(), input);

    assertThat(result.getMessage()).isEqualTo("IP whitelist entry updated");
    assertThat(result.getEntry()).isEqualTo(mapped);
    verify(ipWhitelistService).updateEntry(id, "10.0.0.2", "updated", true);
  }

  @Test
  void deleteIpWhitelistEntry_returnsResponse() {
    UUID id = UUID.randomUUID();

    DeleteIpWhitelistEntryResponse result = service.deleteIpWhitelistEntry(id.toString());

    assertThat(result.getMessage()).isEqualTo("IP whitelist entry deleted");
    assertThat(result.getId()).isEqualTo(id.toString());
    verify(ipWhitelistService).deleteEntry(id);
  }
}

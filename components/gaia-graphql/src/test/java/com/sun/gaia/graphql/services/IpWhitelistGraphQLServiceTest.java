package com.sun.gaia.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.IpWhitelistEntry;
import com.sun.gaia.codegen.types.IpWhitelistEntryInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
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
    when(ipWhitelistService.addEntry("10.0.0.1", "test", false)).thenReturn(entity);

    QueryResult result = service.createIpWhitelistEntry(input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    verify(ipWhitelistService).addEntry("10.0.0.1", "test", false);
  }

  @Test
  void createIpWhitelistEntry_handlesImmutableTrue() {
    IpWhitelistEntryInput input = IpWhitelistEntryInput.newBuilder()
        .pattern("10.0.0.1").immutable(true).build();
    IpWhitelistEntryEntity entity = new IpWhitelistEntryEntity();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    when(ipWhitelistService.addEntry("10.0.0.1", null, true)).thenReturn(entity);

    QueryResult result = service.createIpWhitelistEntry(input);

    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    verify(ipWhitelistService).addEntry("10.0.0.1", null, true);
  }

  @Test
  void updateIpWhitelistEntry_returnsSuccess() {
    UUID id = UUID.randomUUID();
    IpWhitelistEntryInput input = IpWhitelistEntryInput.newBuilder()
        .pattern("10.0.0.2").description("updated").enabled(true).build();

    QueryResult result = service.updateIpWhitelistEntry(id.toString(), input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    verify(ipWhitelistService).updateEntry(id, "10.0.0.2", "updated", true);
  }

  @Test
  void deleteIpWhitelistEntry_returnsSuccess() {
    UUID id = UUID.randomUUID();

    QueryResult result = service.deleteIpWhitelistEntry(id.toString());

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(id.toString());
    verify(ipWhitelistService).deleteEntry(id);
  }
}

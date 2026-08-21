package com.sun.gaia.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.gaia.codegen.types.IpWhitelistEntry;
import com.sun.gaia.codegen.types.IpWhitelistEntryInput;
import com.sun.gaia.codegen.types.QueryResult;
import com.sun.gaia.codegen.types.QuerySuccess;
import com.sun.gaia.graphql.services.IpWhitelistGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpWhitelistDataFetcherTest {

  @Mock private IpWhitelistGraphQLService service;

  @InjectMocks private IpWhitelistDataFetcher fetcher;

  @Test
  void ipWhitelistEntries_shouldDelegate() {
    IpWhitelistEntry entry = IpWhitelistEntry.newBuilder().id("id1").pattern("10.0.0.1").build();
    when(service.ipWhitelistEntries()).thenReturn(List.of(entry));

    List<IpWhitelistEntry> result = fetcher.ipWhitelistEntries();

    assertThat(result).containsExactly(entry);
    verify(service).ipWhitelistEntries();
  }

  @Test
  void createIpWhitelistEntry_shouldDelegate() {
    IpWhitelistEntryInput input = IpWhitelistEntryInput.newBuilder().pattern("10.0.0.1").build();
    QueryResult mock = QuerySuccess.newBuilder().message("IP whitelist entry created").id("id1").build();
    when(service.createIpWhitelistEntry(input)).thenReturn(mock);

    QueryResult result = fetcher.createIpWhitelistEntry(input);

    assertThat(result).isEqualTo(mock);
    verify(service).createIpWhitelistEntry(input);
  }

  @Test
  void updateIpWhitelistEntry_shouldDelegate() {
    IpWhitelistEntryInput input = IpWhitelistEntryInput.newBuilder().pattern("10.0.0.2").build();
    QueryResult mock = QuerySuccess.newBuilder().message("IP whitelist entry updated").id("id1").build();
    when(service.updateIpWhitelistEntry("id1", input)).thenReturn(mock);

    QueryResult result = fetcher.updateIpWhitelistEntry("id1", input);

    assertThat(result).isEqualTo(mock);
    verify(service).updateIpWhitelistEntry("id1", input);
  }

  @Test
  void deleteIpWhitelistEntry_shouldDelegate() {
    QueryResult mock = QuerySuccess.newBuilder().message("IP whitelist entry deleted").id("id1").build();
    when(service.deleteIpWhitelistEntry("id1")).thenReturn(mock);

    QueryResult result = fetcher.deleteIpWhitelistEntry("id1");

    assertThat(result).isEqualTo(mock);
    verify(service).deleteIpWhitelistEntry("id1");
  }
}

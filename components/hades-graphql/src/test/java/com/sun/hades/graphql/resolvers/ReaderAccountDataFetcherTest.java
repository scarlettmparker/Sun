package com.sun.hades.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.DiscordLoginResult;
import com.sun.hades.codegen.types.PaginationInput;
import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.codegen.types.ReaderObjectReference;
import com.sun.hades.codegen.types.RemoteUserInput;
import com.sun.hades.graphql.services.ReaderAccountGraphQLService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReaderAccountDataFetcherTest {

  @Mock private ReaderAccountGraphQLService readerAccountGraphQLService;

  @InjectMocks private ReaderAccountDataFetcher fetcher;

  @Test
  void readerAccount_shouldDelegateToService() {
    ReaderAccount account = ReaderAccount.newBuilder().id("id").discordId("123").build();
    when(readerAccountGraphQLService.readerAccount()).thenReturn(account);

    ReaderAccount result = fetcher.readerAccount();

    assertThat(result).isEqualTo(account);
    verify(readerAccountGraphQLService).readerAccount();
  }

  @Test
  void readerAccounts_shouldDelegateToService() {
    RemoteUserInput input = RemoteUserInput.newBuilder().id("123").build();
    List<RemoteUserInput> inputs = List.of(input);
    ReaderAccount account = ReaderAccount.newBuilder().id("id").discordId("123").build();
    when(readerAccountGraphQLService.readerAccounts(inputs)).thenReturn(List.of(account));

    List<ReaderAccount> result = fetcher.readerAccounts(inputs);

    assertThat(result).containsExactly(account);
    verify(readerAccountGraphQLService).readerAccounts(inputs);
  }

  @Test
  void searchReaderAccounts_shouldDelegateToService() {
    PaginationInput pagination = PaginationInput.newBuilder().page(0).size(10).build();
    ReaderAccount account = ReaderAccount.newBuilder().id("id").build();
    when(readerAccountGraphQLService.searchReaderAccounts("query", pagination)).thenReturn(List.of(account));

    List<ReaderAccount> result = fetcher.searchReaderAccounts("query", pagination);

    assertThat(result).containsExactly(account);
    verify(readerAccountGraphQLService).searchReaderAccounts("query", pagination);
  }

  @Test
  void locateRemoteObjects_shouldDelegateToService() {
    ReaderObjectReference ref = ReaderObjectReference.newBuilder().id("id").build();
    when(readerAccountGraphQLService.locateRemoteObjects(List.of("id"))).thenReturn(List.of(ref));

    List<ReaderObjectReference> result = fetcher.locateRemoteObjects(List.of("id"));

    assertThat(result).containsExactly(ref);
    verify(readerAccountGraphQLService).locateRemoteObjects(List.of("id"));
  }

  @Test
  void discordLogin_shouldDelegateToService() {
    DiscordLoginResult loginResult = DiscordLoginResult.newBuilder()
        .token("token").accountId("aid").readerAccountId("rid").requiresReactivation(false).build();
    when(readerAccountGraphQLService.discordLogin("code", "state")).thenReturn(loginResult);

    DiscordLoginResult result = fetcher.discordLogin("code", "state");

    assertThat(result).isEqualTo(loginResult);
    verify(readerAccountGraphQLService).discordLogin("code", "state");
  }
}

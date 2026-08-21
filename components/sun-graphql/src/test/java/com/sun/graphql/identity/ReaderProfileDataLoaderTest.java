package com.sun.graphql.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.graphql.mappers.ReaderAccountMapper;
import com.sun.hades.model.ReaderAccountEntity;
import com.sun.hades.service.ReaderAccountService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReaderProfileDataLoaderTest {

  @Mock private ReaderAccountService accountService;
  @Mock private ReaderAccountMapper accountMapper;
  @InjectMocks private ReaderProfileDataLoader loader;

  @Test
  void load_shouldBatchAndMapAccounts() throws Exception {
    String discordId1 = "123";
    String discordId2 = "456";

    ReaderAccountEntity e1 = entity(discordId1);
    ReaderAccountEntity e2 = entity(discordId2);
    when(accountService.findByDiscordIds(Set.of(discordId1, discordId2))).thenReturn(List.of(e1, e2));

    ReaderAccount a1 = ReaderAccount.newBuilder().id(e1.getId().toString()).discordId(discordId1).build();
    ReaderAccount a2 = ReaderAccount.newBuilder().id(e2.getId().toString()).discordId(discordId2).build();
    when(accountMapper.map(e1)).thenReturn(a1);
    when(accountMapper.map(e2)).thenReturn(a2);

    Map<String, ReaderAccount> result = loader.load(Set.of(discordId1, discordId2)).get();

    assertThat(result).hasSize(2);
    assertThat(result.get(discordId1)).isEqualTo(a1);
    assertThat(result.get(discordId2)).isEqualTo(a2);
  }

  @Test
  void load_withEmptySet_shouldReturnEmptyMap() throws Exception {
    Map<String, ReaderAccount> result = loader.load(Set.of()).get();

    assertThat(result).isEmpty();
  }

  @Test
  void load_withNullDiscordIdInEntity_shouldSkip() throws Exception {
    ReaderAccountEntity e = new ReaderAccountEntity();
    e.setId(UUID.randomUUID());
    e.setGaiaAccountId(UUID.randomUUID());
    e.setDiscordId(null);
    when(accountService.findByDiscordIds(Set.of("123"))).thenReturn(List.of(e));

    Map<String, ReaderAccount> result = loader.load(Set.of("123")).get();

    assertThat(result).isEmpty();
  }

  @Test
  void load_withSingleId_shouldReturnSingleEntry() throws Exception {
    String discordId = "999";
    ReaderAccountEntity e = entity(discordId);
    when(accountService.findByDiscordIds(Set.of(discordId))).thenReturn(List.of(e));
    ReaderAccount a = ReaderAccount.newBuilder().id(e.getId().toString()).discordId(discordId).build();
    when(accountMapper.map(e)).thenReturn(a);

    Map<String, ReaderAccount> result = loader.load(Set.of(discordId)).get();

    assertThat(result).containsKey(discordId);
    assertThat(result.get(discordId)).isEqualTo(a);
  }

  private ReaderAccountEntity entity(String discordId) {
    ReaderAccountEntity e = new ReaderAccountEntity();
    e.setId(UUID.randomUUID());
    e.setGaiaAccountId(UUID.randomUUID());
    e.setDiscordId(discordId);
    e.setDiscordUsername("user-" + discordId);
    return e;
  }
}

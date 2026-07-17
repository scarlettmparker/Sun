package com.sun.graphql.identity;

import com.netflix.graphql.dgs.DgsDataLoader;
import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.graphql.mappers.ReaderAccountMapper;
import com.sun.hades.model.ReaderAccountEntity;
import com.sun.hades.service.ReaderAccountService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.dataloader.MappedBatchLoader;

/**
 * Batch-resolves reader-account profiles by Discord id, one backend call per
 * GraphQL request regardless of how many authors appear.
 */
@DgsDataLoader(name = "readerProfile")
public class ReaderProfileDataLoader implements MappedBatchLoader<String, ReaderAccount> {

  private final ReaderAccountService accountService;
  private final ReaderAccountMapper accountMapper;

  public ReaderProfileDataLoader(
      ReaderAccountService accountService, ReaderAccountMapper accountMapper) {
    this.accountService = accountService;
    this.accountMapper = accountMapper;
  }

  @Override
  public CompletableFuture<Map<String, ReaderAccount>> load(Set<String> discordIds) {
    Map<String, ReaderAccount> byDiscordId = new HashMap<>();
    if (discordIds != null && !discordIds.isEmpty()) {
      for (ReaderAccountEntity entity : accountService.findByDiscordIds(discordIds)) {
        if (entity.getDiscordId() != null) {
          byDiscordId.put(entity.getDiscordId(), accountMapper.map(entity));
        }
      }
    }
    return CompletableFuture.completedFuture(byDiscordId);
  }
}

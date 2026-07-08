package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.model.ReaderAccountEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader account entities.
 */
@Component
public class ReaderAccountMapper {

  /**
   * Maps a reader account entity to the GraphQL ReaderAccount type.
   *
   * @param entity the reader account entity
   * @return the GraphQL ReaderAccount
   */
  public ReaderAccount map(ReaderAccountEntity entity) {
    return ReaderAccount.newBuilder()
        .id(entity.getId().toString())
        .gaiaAccountId(entity.getGaiaAccountId().toString())
        .discordId(entity.getDiscordId())
        .discordUsername(entity.getDiscordUsername())
        .globalName(entity.getGlobalName())
        .avatar(entity.getAvatar())
        .cefrLevel(entity.getCefrLevel())
        .build();
  }
}

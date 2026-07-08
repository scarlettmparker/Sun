package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.model.ReaderAccountEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapper for reader account entities.
 */
@Component
public class ReaderAccountMapper {

  private static final Logger logger = LoggerFactory.getLogger(ReaderAccountMapper.class);

  /**
   * Maps a reader account entity to the GraphQL ReaderAccount type.
   *
   * @param entity the reader account entity
   * @return the GraphQL ReaderAccount
   */
  public ReaderAccount map(ReaderAccountEntity entity) {
    logger.debug("Mapping reader account {}", entity.getDiscordId());
    ReaderAccount account = ReaderAccount.newBuilder()
        .id(entity.getId().toString())
        .gaiaAccountId(entity.getGaiaAccountId().toString())
        .discordId(entity.getDiscordId())
        .discordUsername(entity.getDiscordUsername())
        .globalName(entity.getGlobalName())
        .avatar(entity.getAvatar())
        .cefrLevel(entity.getCefrLevel())
        .build();
    logger.debug("Mapped reader account with id {}", account.getId());
    return account;
  }
}

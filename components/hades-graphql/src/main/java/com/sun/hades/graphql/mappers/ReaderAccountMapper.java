package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.ReaderAccount;
import com.sun.hades.codegen.types.ReaderRole;
import com.sun.hades.model.ReaderAccountEntity;
import com.sun.hades.service.ReaderLevels;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
        .roles(mapRoles(entity.getGuildRoles()))
        .build();
    logger.debug("Mapped reader account with id {}", account.getId());
    return account;
  }

  /**
   * Resolves stored level keys into GraphQL reader roles.
   *
   * @param keys the stored level keys
   * @return the resolved roles
   */
  private List<ReaderRole> mapRoles(List<String> keys) {
    if (keys == null) {
      return List.of();
    }
    return keys.stream()
        .map(ReaderLevels.BY_KEY::get)
        .filter(Objects::nonNull)
        .map(level -> ReaderRole.newBuilder()
            .key(level.key())
            .name(level.name())
            .cefrLevel(level.cefrLevel())
            .build())
        .collect(Collectors.toList());
  }
}

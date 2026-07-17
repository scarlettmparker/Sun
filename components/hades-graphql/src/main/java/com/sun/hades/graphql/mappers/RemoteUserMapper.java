package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.codegen.types.RemoteUserType;
import org.springframework.stereotype.Component;

/**
 * Mapper for remote-user references.
 */
@Component
public class RemoteUserMapper {

  /**
   * Builds a Discord remote-user reference from a Discord id.
   *
   * @param discordId the Discord user id
   * @return the Discord RemoteUser reference
   */
  public RemoteUser discord(String discordId) {
    return RemoteUser.newBuilder()
        .type(RemoteUserType.DISCORD)
        .id(discordId)
        .build();
  }
}

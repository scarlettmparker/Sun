package com.sun.gaia.graphql.mappers;

import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.codegen.types.RemoteUser;
import com.sun.gaia.codegen.types.RemoteUserType;
import com.sun.gaia.model.AccountEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting account entities to GraphQL types.
 */
@Component
public class AccountMapper {

  /**
   * Maps an account entity to the GraphQL Account type.
   *
   * @param entity the account entity
   * @return the GraphQL Account
   */
  public Account map(AccountEntity entity) {
    return Account.newBuilder()
        .id(entity.getId().toString())
        .username(entity.getUsername())
        .personId(entity.getPersonId().toString())
        .status(entity.getStatus())
        .provider(entity.getProvider())
        .remoteUsers(remoteUsers(entity))
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }

  /**
   * Derives the remote-user identities for an account from its provider columns.
   *
   * @param entity the account entity
   * @return the remote users, or null when the account has no provider
   */
  private List<RemoteUser> remoteUsers(AccountEntity entity) {
    String provider = entity.getProvider();
    String providerId = entity.getProviderId();
    if (provider == null || providerId == null) {
      return null;
    }
    if ("discord".equals(provider)) {
      return List.of(RemoteUser.newBuilder()
          .type(RemoteUserType.DISCORD)
          .id(providerId)
          .build());
    }
    return List.of();
  }
}

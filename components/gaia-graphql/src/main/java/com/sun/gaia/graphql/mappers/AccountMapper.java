package com.sun.gaia.graphql.mappers;

import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.model.AccountEntity;
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
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getLastUpdatedAt())
        .build();
  }
}

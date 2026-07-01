package com.sun.gaia.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.gaia.codegen.types.Account;
import com.sun.gaia.model.AccountEntity;
import com.sun.gaia.model.enums.AccountStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountMapperTest {

  private final AccountMapper mapper = new AccountMapper();

  @Test
  void map_entityToGraphQL() {
    UUID id = UUID.randomUUID();
    UUID personId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();

    AccountEntity entity = new AccountEntity();
    entity.setId(id);
    entity.setUsername("testuser");
    entity.setPersonId(personId);
    entity.setStatus(AccountStatus.ACTIVE);
    entity.setProvider("local");
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(updatedAt);

    Account result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getUsername()).isEqualTo("testuser");
    assertThat(result.getPersonId()).isEqualTo(personId.toString());
    assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(result.getProvider()).isEqualTo("local");
    assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
  }
}

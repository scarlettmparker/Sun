package com.sun.hades.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.model.ReaderAccountEntity;
import com.sun.hades.model.enums.CefrLevel;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReaderAccountMapperTest {

  private final ReaderAccountMapper mapper = new ReaderAccountMapper();

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    UUID gaiaAccountId = UUID.randomUUID();
    ReaderAccountEntity entity = new ReaderAccountEntity();
    entity.setId(id);
    entity.setGaiaAccountId(gaiaAccountId);
    entity.setDiscordId("123");
    entity.setDiscordUsername("user");
    entity.setGlobalName("User");
    entity.setAvatar("avatar.png");
    entity.setCefrLevel(CefrLevel.B2);
    entity.setGuildRoles(List.of());

    var result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getGaiaAccountId()).isEqualTo(gaiaAccountId.toString());
    assertThat(result.getDiscordId()).isEqualTo("123");
    assertThat(result.getDiscordUsername()).isEqualTo("user");
    assertThat(result.getGlobalName()).isEqualTo("User");
    assertThat(result.getAvatar()).isEqualTo("avatar.png");
    assertThat(result.getCefrLevel()).isEqualTo(CefrLevel.B2);
    assertThat(result.getRoles()).isEmpty();
  }

  @Test
  void map_shouldTolerateNullRoles() {
    ReaderAccountEntity entity = new ReaderAccountEntity();
    entity.setId(UUID.randomUUID());
    entity.setGaiaAccountId(UUID.randomUUID());
    entity.setDiscordId("123");
    entity.setGuildRoles(null);

    var result = mapper.map(entity);

    assertThat(result.getRoles()).isEmpty();
  }
}

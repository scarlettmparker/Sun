package com.sun.hades.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.codegen.types.RemoteUserType;
import org.junit.jupiter.api.Test;

class RemoteUserMapperTest {

  private final RemoteUserMapper mapper = new RemoteUserMapper();

  @Test
  void discord_shouldBuildDiscordReference() {
    var result = mapper.discord("12345");

    assertThat(result.getType()).isEqualTo(RemoteUserType.DISCORD);
    assertThat(result.getId()).isEqualTo("12345");
  }
}

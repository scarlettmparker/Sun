package com.sun.hades.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.model.ReaderSourceEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReaderSourceMapperTest {

  private final ReaderSourceMapper mapper = new ReaderSourceMapper();

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    ReaderSourceEntity entity = new ReaderSourceEntity();
    entity.setId(id);
    entity.setName("Wikipedia");
    entity.setUrl("https://en.wikipedia.org");

    var result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getName()).isEqualTo("Wikipedia");
    assertThat(result.getUrl()).isEqualTo("https://en.wikipedia.org");
  }
}

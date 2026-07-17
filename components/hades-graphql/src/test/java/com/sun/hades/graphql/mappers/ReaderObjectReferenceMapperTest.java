package com.sun.hades.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.service.RemoteObjectReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReaderObjectReferenceMapperTest {

  private final ReaderObjectReferenceMapper mapper = new ReaderObjectReferenceMapper();

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    RemoteObjectReference reference = new RemoteObjectReference(id, "ANNOTATION", ownerId, null);

    var result = mapper.map(reference);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getOwnerType()).isEqualTo("ANNOTATION");
    assertThat(result.getOwnerId()).isEqualTo(ownerId.toString());
  }
}

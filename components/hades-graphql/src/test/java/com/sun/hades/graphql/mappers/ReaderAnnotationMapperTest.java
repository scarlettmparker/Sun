package com.sun.hades.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.codegen.types.ReaderPosition;
import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.codegen.types.RemoteUserType;
import com.sun.hades.model.ReaderAnnotationEntity;
import com.sun.hades.model.enums.ReaderStatus;
import com.sun.hades.model.enums.VoteValue;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReaderAnnotationMapperTest {

  private final ReaderAnnotationMapper mapper = new ReaderAnnotationMapper();

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    UUID positionId = UUID.randomUUID();
    ReaderAnnotationEntity entity = new ReaderAnnotationEntity();
    entity.setId(id);
    entity.setPositionId(positionId);
    entity.setBody("body");
    entity.setStatus(ReaderStatus.ACTIVE);
    entity.setUpvotes(4);
    entity.setDownvotes(1);
    entity.setRemoteObject(List.of("hades:text:abc"));
    ReaderPosition position = ReaderPosition.newBuilder()
        .id(positionId.toString()).build();
    RemoteUser author = RemoteUser.newBuilder()
        .type(RemoteUserType.DISCORD).id("123").build();

    var result = mapper.map(entity, position, author, 3, VoteValue.UP);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getPositionId()).isEqualTo(positionId.toString());
    assertThat(result.getBody()).isEqualTo("body");
    assertThat(result.getStatus()).isEqualTo(ReaderStatus.ACTIVE);
    assertThat(result.getUpvotes()).isEqualTo(4);
    assertThat(result.getDownvotes()).isEqualTo(1);
    assertThat(result.getNetScore()).isEqualTo(3);
    assertThat(result.getReplyCount()).isEqualTo(3);
    assertThat(result.getRemoteObject()).containsExactly("hades:text:abc");
    assertThat(result.getPosition()).isEqualTo(position);
    assertThat(result.getAuthor().getId()).isEqualTo("123");
    assertThat(result.getMyVote()).isEqualTo(VoteValue.UP);
  }

  @Test
  void map_shouldDefaultReplyCountToZero() {
    ReaderAnnotationEntity entity = new ReaderAnnotationEntity();
    entity.setId(UUID.randomUUID());
    entity.setPositionId(UUID.randomUUID());
    entity.setBody("body");
    entity.setStatus(ReaderStatus.ACTIVE);
    entity.setUpvotes(0);
    entity.setDownvotes(0);

    var result = mapper.map(entity, null, null, 0, null);

    assertThat(result.getReplyCount()).isZero();
    assertThat(result.getNetScore()).isZero();
    assertThat(result.getPosition()).isNull();
    assertThat(result.getAuthor()).isNull();
    assertThat(result.getMyVote()).isNull();
  }
}

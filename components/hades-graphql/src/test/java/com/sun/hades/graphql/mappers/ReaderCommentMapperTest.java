package com.sun.hades.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.codegen.types.RemoteUser;
import com.sun.hades.codegen.types.RemoteUserType;
import com.sun.hades.model.ReaderCommentEntity;
import com.sun.hades.model.enums.ReaderStatus;
import com.sun.hades.model.enums.VoteValue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReaderCommentMapperTest {

  private final ReaderCommentMapper mapper = new ReaderCommentMapper();

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    UUID annotationId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    ReaderCommentEntity entity = new ReaderCommentEntity();
    entity.setId(id);
    entity.setAnnotationId(annotationId);
    entity.setParentId(parentId);
    entity.setBody("body");
    entity.setStatus(ReaderStatus.ACTIVE);
    entity.setUpvotes(2);
    entity.setDownvotes(1);
    RemoteUser author = RemoteUser.newBuilder()
        .type(RemoteUserType.DISCORD).id("123").build();

    var result = mapper.map(entity, author, VoteValue.UP);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getAnnotationId()).isEqualTo(annotationId.toString());
    assertThat(result.getParentId()).isEqualTo(parentId.toString());
    assertThat(result.getBody()).isEqualTo("body");
    assertThat(result.getStatus()).isEqualTo(ReaderStatus.ACTIVE);
    assertThat(result.getNetScore()).isEqualTo(1);
    assertThat(result.getAuthor().getId()).isEqualTo("123");
    assertThat(result.getMyVote()).isEqualTo(VoteValue.UP);
  }

  @Test
  void map_shouldNullOutMissingOptionalFields() {
    ReaderCommentEntity entity = new ReaderCommentEntity();
    entity.setId(UUID.randomUUID());
    entity.setAnnotationId(UUID.randomUUID());
    entity.setBody("body");
    entity.setStatus(ReaderStatus.ACTIVE);
    entity.setUpvotes(0);
    entity.setDownvotes(0);

    var result = mapper.map(entity, null, null);

    assertThat(result.getParentId()).isNull();
    assertThat(result.getAuthor()).isNull();
    assertThat(result.getMyVote()).isNull();
  }
}

package com.sun.icarus.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.icarus.codegen.types.ForumPost;
import com.sun.icarus.codegen.types.RemoteUser;
import com.sun.icarus.codegen.types.RemoteUserType;
import com.sun.icarus.model.ForumPostEntity;
import com.sun.icarus.model.enums.PostStatus;
import com.sun.icarus.model.enums.VoteValue;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForumPostMapperTest {

  private final ForumPostMapper mapper = new ForumPostMapper();

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    UUID threadId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 16, 11, 0);
    RemoteUser author = RemoteUser.newBuilder().type(RemoteUserType.DISCORD).id("123").build();

    ForumPostEntity entity = new ForumPostEntity();
    entity.setId(id);
    entity.setThreadId(threadId);
    entity.setParentId(parentId);
    entity.setBody("hello world");
    entity.setStatus(PostStatus.ACTIVE);
    entity.setUpvotes(5);
    entity.setDownvotes(2);
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(updatedAt);

    ForumPost result = mapper.map(entity, author, VoteValue.UP);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getThreadId()).isEqualTo(threadId.toString());
    assertThat(result.getParentId()).isEqualTo(parentId.toString());
    assertThat(result.getBody()).isEqualTo("hello world");
    assertThat(result.getStatus()).isEqualTo(PostStatus.ACTIVE);
    assertThat(result.getUpvotes()).isEqualTo(5);
    assertThat(result.getDownvotes()).isEqualTo(2);
    assertThat(result.getNetScore()).isEqualTo(3);
    assertThat(result.getAuthor()).isEqualTo(author);
    assertThat(result.getMyVote()).isEqualTo(VoteValue.UP);
    assertThat(result.getCreatedAt()).isNotNull();
    assertThat(result.getUpdatedAt()).isNotNull();
  }

  @Test
  void map_withNullParentAndNoVote_shouldMapNulls() {
    UUID id = UUID.randomUUID();
    UUID threadId = UUID.randomUUID();

    ForumPostEntity entity = new ForumPostEntity();
    entity.setId(id);
    entity.setThreadId(threadId);
    entity.setParentId(null);
    entity.setBody("body");
    entity.setStatus(PostStatus.ACTIVE);
    entity.setUpvotes(0);
    entity.setDownvotes(0);

    ForumPost result = mapper.map(entity, null, null);

    assertThat(result.getParentId()).isNull();
    assertThat(result.getAuthor()).isNull();
    assertThat(result.getMyVote()).isNull();
    assertThat(result.getNetScore()).isZero();
    assertThat(result.getCreatedAt()).isNull();
    assertThat(result.getUpdatedAt()).isNull();
  }

}

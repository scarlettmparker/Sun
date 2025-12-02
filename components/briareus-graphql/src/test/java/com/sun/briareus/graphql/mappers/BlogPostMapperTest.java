package com.sun.briareus.graphql.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.briareus.model.PostEntity;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogPostInput;

@ExtendWith(MockitoExtension.class)
class BlogPostMapperTest {

  private BlogPostMapper blogPostMapper = new BlogPostMapper();

  @Test
  void map_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2023, 1, 1, 10, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2023, 1, 2, 11, 0);

    PostEntity postEntity = new PostEntity();
    postEntity.setId(UUID.randomUUID());
    postEntity.setTitle("Test Title");
    postEntity.setContent("Test Content");
    postEntity.setTags(Arrays.asList("tag1", "tag2"));
    postEntity.setCreatedAt(createdAt);
    postEntity.setLastUpdatedAt(updatedAt);

    BlogPost result = blogPostMapper.map(postEntity);

    assertThat(result.getId()).isEqualTo(postEntity.getId().toString());
    assertThat(result.getTitle()).isEqualTo("Test Title");
    assertThat(result.getContent()).isEqualTo("Test Content");
    assertThat(result.getTags()).containsExactly("tag1", "tag2");
    assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void mapInput_shouldMapAllFields() {
    BlogPostInput input = BlogPostInput.newBuilder()
        .content("Test Content")
        .tags(Arrays.asList("tag1", "tag2"))
        .build();

    PostEntity result = blogPostMapper.mapInput("Test Title", input);

    assertThat(result.getTitle()).isEqualTo("Test Title");
    assertThat(result.getContent()).isEqualTo("Test Content");
    assertThat(result.getTags()).containsExactly("tag1", "tag2");
  }
}
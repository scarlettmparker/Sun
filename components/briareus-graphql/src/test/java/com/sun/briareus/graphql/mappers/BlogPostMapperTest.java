package com.sun.briareus.graphql.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sun.briareus.model.BlogPostTypeEntity;
import com.sun.briareus.model.PostEntity;
import com.sun.briareus.service.BlogPostTypeService;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogPostInput;
import com.sun.briareus.codegen.types.BlogPostType;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class BlogPostMapperTest {

  @Mock
  private BlogPostTypeMapper blogPostTypeMapper;

  @Mock
  private BlogPostTypeService blogPostTypeService;

  @InjectMocks
  private BlogPostMapper blogPostMapper;

  @Test
  void map_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2023, 1, 1, 10, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2023, 1, 2, 11, 0);
    BlogPostTypeEntity typeEntity = new BlogPostTypeEntity();
    typeEntity.setName("BOT_FAQ");
    BlogPostType type = BlogPostType.newBuilder().id("1").name("BOT_FAQ").build();

    PostEntity postEntity = new PostEntity();
    postEntity.setId(UUID.randomUUID());
    postEntity.setTitle("Test Title");
    postEntity.setContent("Test Content");
    postEntity.setTags(Arrays.asList("tag1", "tag2"));
    postEntity.setLanguage("en");
    postEntity.setType(typeEntity);
    postEntity.setCreatedAt(createdAt);
    postEntity.setLastUpdatedAt(updatedAt);
    when(blogPostTypeMapper.map(typeEntity)).thenReturn(type);

    BlogPost result = blogPostMapper.map(postEntity);

    assertThat(result.getId()).isEqualTo(postEntity.getId().toString());
    assertThat(result.getTitle()).isEqualTo("Test Title");
    assertThat(result.getContent()).isEqualTo("Test Content");
    assertThat(result.getTags()).containsExactly("tag1", "tag2");
    assertThat(result.getLanguage()).isEqualTo("en");
    assertThat(result.getType()).isEqualTo(type);
    assertThat(result.getCreatedAt()).isEqualTo(createdAt.atOffset(ZoneOffset.UTC));
    assertThat(result.getUpdatedAt()).isEqualTo(updatedAt.atOffset(ZoneOffset.UTC));
  }

  @Test
  void map_shouldOmitTypeAndLanguageWhenAbsent() {
    PostEntity postEntity = new PostEntity();
    postEntity.setId(UUID.randomUUID());
    postEntity.setTitle("Test Title");

    BlogPost result = blogPostMapper.map(postEntity);

    assertThat(result.getType()).isNull();
    assertThat(result.getLanguage()).isNull();
  }

  @Test
  void mapInput_shouldMapAllFields() {
    BlogPostTypeEntity typeEntity = new BlogPostTypeEntity();
    typeEntity.setName("BOT_FAQ");
    BlogPostInput input = BlogPostInput.newBuilder()
        .content("Test Content")
        .tags(Arrays.asList("tag1", "tag2"))
        .typeId("b9f70000-0000-4000-8000-000000000001")
        .language("en")
        .build();
    when(blogPostTypeService.findById(any(UUID.class))).thenReturn(Optional.of(typeEntity));

    PostEntity result = blogPostMapper.mapInput("Test Title", input);

    assertThat(result.getTitle()).isEqualTo("Test Title");
    assertThat(result.getContent()).isEqualTo("Test Content");
    assertThat(result.getTags()).containsExactly("tag1", "tag2");
    assertThat(result.getLanguage()).isEqualTo("en");
    assertThat(result.getType()).isEqualTo(typeEntity);
  }

  @Test
  void mapInput_shouldThrowWhenTypeNotFound() {
    BlogPostInput input = BlogPostInput.newBuilder()
        .typeId("b9f70000-0000-4000-8000-000000000001")
        .build();
    when(blogPostTypeService.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> blogPostMapper.mapInput("Test Title", input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Blog post type not found");
  }
}

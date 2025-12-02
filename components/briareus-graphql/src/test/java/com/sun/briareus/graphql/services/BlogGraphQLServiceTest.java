package com.sun.briareus.graphql.services;

import com.sun.briareus.graphql.mappers.BlogPostMapper;
import com.sun.briareus.service.BriareusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sun.briareus.model.PostEntity;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogPostInput;
import com.sun.cerberus.codegen.types.QueryResult;
import com.sun.cerberus.codegen.types.QuerySuccess;
import com.sun.cerberus.codegen.types.StandardError;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class BlogGraphQLServiceTest {

  @Mock
  private BriareusService briareusService;

  @Mock
  private BlogPostMapper blogPostMapper;

  @InjectMocks
  private BlogGraphQLService blogGraphQLService;

  private PostEntity postEntity1;
  private PostEntity postEntity2;
  private BlogPost blogPost1;
  private BlogPost blogPost2;

  @BeforeEach
  void setUp() {
    postEntity1 = new PostEntity();
    postEntity1.setId(UUID.randomUUID());
    postEntity1.setTitle("Test Blog Post 1");
    postEntity1.setContent("This is the content of the first blog post.");
    postEntity1.setTags(Arrays.asList("tag1", "tag2"));

    postEntity2 = new PostEntity();
    postEntity2.setId(UUID.randomUUID());
    postEntity2.setTitle("Test Blog Post 2");
    postEntity2.setContent("This is the content of the second blog post.");
    postEntity2.setTags(Arrays.asList());

    blogPost1 = BlogPost.newBuilder()
        .id(postEntity1.getId().toString())
        .title("Test Blog Post 1")
        .content("This is the content of the first blog post.")
        .tags(Arrays.asList("tag1", "tag2"))
        .build();

    blogPost2 = BlogPost.newBuilder()
        .id(postEntity2.getId().toString())
        .title("Test Blog Post 2")
        .content("This is the content of the second blog post.")
        .tags(Arrays.asList())
        .build();
  }

  @Test
  void listBlogPosts_shouldReturnMappedBlogPosts() {
    List<PostEntity> postEntities = Arrays.asList(postEntity1, postEntity2);
    when(briareusService.listPosts()).thenReturn(postEntities);
    when(blogPostMapper.map(postEntity1)).thenReturn(blogPost1);
    when(blogPostMapper.map(postEntity2)).thenReturn(blogPost2);
    List<BlogPost> result = blogGraphQLService.listBlogPosts();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("Test Blog Post 1");
    assertThat(result.get(0).getContent()).isEqualTo("This is the content of the first blog post.");
    assertThat(result.get(0).getTags()).containsExactly("tag1", "tag2");
    assertThat(result.get(1).getTitle()).isEqualTo("Test Blog Post 2");
    assertThat(result.get(1).getContent()).isEqualTo("This is the content of the second blog post.");
    assertThat(result.get(1).getTags()).isEmpty();
  }

  @Test
  void listBlogPosts_shouldReturnEmptyListWhenNoPosts() {
    when(briareusService.listPosts()).thenReturn(Arrays.asList());
    List<BlogPost> result = blogGraphQLService.listBlogPosts();
    assertThat(result).isEmpty();
  }

  @Test
  void locateBlogPost_shouldReturnMappedBlogPost() {
    when(briareusService.locatePost(postEntity1.getId())).thenReturn(java.util.Optional.of(postEntity1));
    when(blogPostMapper.map(postEntity1)).thenReturn(blogPost1);
    BlogPost result = blogGraphQLService.locateBlogPost(postEntity1.getId().toString());
    assertThat(result.getTitle()).isEqualTo("Test Blog Post 1");
    assertThat(result.getContent()).isEqualTo("This is the content of the first blog post.");
    assertThat(result.getTags()).containsExactly("tag1", "tag2");
  }

  @Test
  void locateBlogPost_shouldThrowExceptionWhenPostNotFound() {
    when(briareusService.locatePost(postEntity1.getId())).thenReturn(java.util.Optional.empty());
    assertThatThrownBy(() -> blogGraphQLService.locateBlogPost(postEntity1.getId().toString()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Blog post not found with id: " + postEntity1.getId().toString());
  }

  @Test
  void createBlogPost_shouldReturnQuerySuccessWhenSuccessful() {
    BlogPostInput input = BlogPostInput.newBuilder()
        .content("New blog content")
        .tags(Arrays.asList("new", "blog"))
        .build();

    PostEntity postEntity = new PostEntity();
    postEntity.setTitle("New Blog Post");
    postEntity.setContent("New blog content");
    postEntity.setTags(Arrays.asList("new", "blog"));

    PostEntity savedEntity = new PostEntity();
    savedEntity.setId(UUID.randomUUID());
    savedEntity.setTitle("New Blog Post");
    savedEntity.setContent("New blog content");
    savedEntity.setTags(Arrays.asList("new", "blog"));

    when(blogPostMapper.mapInput("New Blog Post", input)).thenReturn(postEntity);
    when(briareusService.save(postEntity)).thenReturn(savedEntity);

    QueryResult result = blogGraphQLService.createBlogPost("New Blog Post", input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
    QuerySuccess success = (QuerySuccess) result;
    assertThat(success.getSuccess()).isTrue();
  }

  @Test
  void createBlogPost_shouldReturnStandardErrorWhenExceptionOccurs() {
    BlogPostInput input = BlogPostInput.newBuilder()
        .content("New blog content")
        .tags(Arrays.asList("new", "blog"))
        .build();

    PostEntity postEntity = new PostEntity();
    postEntity.setTitle("New Blog Post");
    postEntity.setContent("New blog content");
    postEntity.setTags(Arrays.asList("new", "blog"));

    when(blogPostMapper.mapInput("New Blog Post", input)).thenReturn(postEntity);
    doThrow(new RuntimeException("Database error")).when(briareusService).save(postEntity);

    QueryResult result = blogGraphQLService.createBlogPost("New Blog Post", input);

    assertThat(result).isInstanceOf(StandardError.class);
    StandardError error = (StandardError) result;
    assertThat(error.getMessage()).contains("Failed to create blog post: Database error");
    assertThat(error.getId()).isNull();
  }
}
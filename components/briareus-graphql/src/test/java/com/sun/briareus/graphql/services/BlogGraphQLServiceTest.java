package com.sun.briareus.graphql.services;

import com.sun.briareus.graphql.mappers.BlogPostMapper;
import com.sun.briareus.graphql.mappers.BlogPostTypeMapper;
import com.sun.briareus.model.BlogPostTypeEntity;
import com.sun.briareus.model.PostEntity;
import com.sun.briareus.service.BlogPostTypeService;
import com.sun.briareus.service.BriareusService;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogPostInput;
import com.sun.briareus.codegen.types.BlogPostType;
import com.sun.briareus.codegen.types.PagedBlogPosts;
import com.sun.briareus.codegen.types.PaginationInput;
import com.sun.briareus.codegen.types.QueryResult;
import com.sun.briareus.codegen.types.QuerySuccess;
import com.sun.briareus.codegen.types.StandardError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogGraphQLServiceTest {

  @Mock
  private BriareusService briareusService;

  @Mock
  private BlogPostTypeService blogPostTypeService;

  @Mock
  private BlogPostMapper blogPostMapper;

  @Mock
  private BlogPostTypeMapper blogPostTypeMapper;

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
    Page<PostEntity> page = new PageImpl<>(postEntities, PageRequest.of(0, 10, Sort.by("title")), 2);

    when(briareusService.listPostsPaged(any(), any(Pageable.class))).thenReturn(page);
    when(blogPostMapper.map(postEntity1)).thenReturn(blogPost1);
    when(blogPostMapper.map(postEntity2)).thenReturn(blogPost2);

    PagedBlogPosts result = blogGraphQLService.listBlogPosts(PaginationInput.newBuilder().build());

    assertThat(result.getItems()).hasSize(2);
    assertThat(result.getPageInfo().getTotalCount()).isEqualTo(2);
  }

  @Test
  void listBlogPosts_shouldReturnEmptyPageWhenNoPosts() {
    Page<PostEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(briareusService.listPostsPaged(any(), any(Pageable.class))).thenReturn(page);

    PagedBlogPosts result = blogGraphQLService.listBlogPosts(null);

    assertThat(result.getItems()).isEmpty();
    assertThat(result.getPageInfo().getTotalCount()).isZero();
  }

  @Test
  void locateBlogPost_shouldReturnMappedBlogPost() {
    when(briareusService.locatePost(postEntity1.getId())).thenReturn(Optional.of(postEntity1));
    when(blogPostMapper.map(postEntity1)).thenReturn(blogPost1);

    BlogPost result = blogGraphQLService.locateBlogPost(postEntity1.getId().toString());

    assertThat(result.getTitle()).isEqualTo("Test Blog Post 1");
  }

  @Test
  void locateBlogPost_shouldThrowExceptionWhenPostNotFound() {
    when(briareusService.locatePost(postEntity1.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> blogGraphQLService.locateBlogPost(postEntity1.getId().toString()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Blog post not found with id: " + postEntity1.getId().toString());
  }

  @Test
  void blogPostTypes_shouldReturnMappedTypes() {
    BlogPostTypeEntity entity = new BlogPostTypeEntity();
    entity.setName("BOT_FAQ");
    BlogPostType type = BlogPostType.newBuilder().id("1").name("BOT_FAQ").build();
    when(blogPostTypeService.findAll()).thenReturn(List.of(entity));
    when(blogPostTypeMapper.map(List.of(entity))).thenReturn(List.of(type));

    assertThat(blogGraphQLService.blogPostTypes()).containsExactly(type);
  }

  @Test
  void createBlogPostType_shouldReturnQuerySuccess() {
    BlogPostTypeEntity entity = new BlogPostTypeEntity();
    entity.setName("BOT_FAQ");
    BlogPostTypeEntity saved = new BlogPostTypeEntity();
    saved.setId(UUID.randomUUID());
    saved.setName("BOT_FAQ");
    when(blogPostTypeService.findByName("BOT_FAQ")).thenReturn(Optional.empty());
    when(blogPostTypeService.save(any(BlogPostTypeEntity.class))).thenReturn(saved);

    QueryResult result = blogGraphQLService.createBlogPostType("BOT_FAQ", "description");

    assertThat(result).isInstanceOf(QuerySuccess.class);
    assertThat(((QuerySuccess) result).getId()).isEqualTo(saved.getId().toString());
  }

  @Test
  void createBlogPostType_shouldReturnErrorWhenNameBlank() {
    QueryResult result = blogGraphQLService.createBlogPostType("  ", null);

    assertThat(result).isInstanceOf(StandardError.class);
  }

  @Test
  void createBlogPostType_shouldReturnErrorWhenDuplicate() {
    when(blogPostTypeService.findByName("BOT_FAQ")).thenReturn(Optional.of(new BlogPostTypeEntity()));

    QueryResult result = blogGraphQLService.createBlogPostType("BOT_FAQ", null);

    assertThat(result).isInstanceOf(StandardError.class);
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

    when(blogPostMapper.mapInput("New Blog Post", input)).thenReturn(postEntity);
    when(briareusService.save(postEntity)).thenReturn(savedEntity);

    QueryResult result = blogGraphQLService.createBlogPost("New Blog Post", input);

    assertThat(result).isInstanceOf(QuerySuccess.class);
  }

  @Test
  void createBlogPost_shouldReturnStandardErrorWhenExceptionOccurs() {
    BlogPostInput input = BlogPostInput.newBuilder()
        .content("New blog content")
        .tags(Arrays.asList("new", "blog"))
        .build();

    PostEntity postEntity = new PostEntity();
    postEntity.setTitle("New Blog Post");

    when(blogPostMapper.mapInput("New Blog Post", input)).thenReturn(postEntity);
    doThrow(new RuntimeException("Database error")).when(briareusService).save(postEntity);

    QueryResult result = blogGraphQLService.createBlogPost("New Blog Post", input);

    assertThat(result).isInstanceOf(StandardError.class);
    assertThat(((StandardError) result).getMessage()).contains("Database error");
  }
}

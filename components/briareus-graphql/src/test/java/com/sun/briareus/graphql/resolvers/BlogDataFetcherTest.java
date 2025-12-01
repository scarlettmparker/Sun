package com.sun.briareus.graphql.resolvers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.briareus.graphql.services.BlogGraphQLService;
import com.sun.briareus.codegen.types.BlogPost;

@ExtendWith(MockitoExtension.class)
class BlogDataFetcherTest {

  @Mock
  private BlogGraphQLService blogGraphQLService;

  @InjectMocks
  private BlogDataFetcher blogDataFetcher;

  private List<BlogPost> mockBlogPosts;

  @BeforeEach
  void setUp() {
    BlogPost blogPost1 = BlogPost.newBuilder()
        .id("1")
        .title("Test Blog 1")
        .content("Test Content 1")
        .tags(Arrays.asList("Tag1", "Tag2"))
        .build();

    BlogPost blogPost2 = BlogPost.newBuilder()
        .id("2")
        .title("Test Blog 2")
        .content("Test Content 2")
        .tags(Arrays.asList("Tag3", "Tag4"))
        .build();

    mockBlogPosts = Arrays.asList(blogPost1, blogPost2);
  }

  @Test
  void listBlogPosts_shouldReturnBlogPostsFromService() {
    when(blogGraphQLService.listBlogPosts()).thenReturn(mockBlogPosts);
    List<BlogPost> result = blogDataFetcher.listBlogPosts();
    assertThat(result).isEqualTo(mockBlogPosts);
    assertThat(result).hasSize(2);

    // First blog post
    assertThat(result.get(0).getTitle()).isEqualTo("Test Blog 1");
    assertThat(result.get(0).getContent()).isEqualTo("Test Content 1");
    assertThat(result.get(0).getTags()).containsExactly("Tag1", "Tag2");

    // Second blog post
    assertThat(result.get(1).getTitle()).isEqualTo("Test Blog 2");
    assertThat(result.get(1).getContent()).isEqualTo("Test Content 2");
    assertThat(result.get(1).getTags()).containsExactly("Tag3", "Tag4");
  }

  @Test
  void locateBlogPost_shouldReturnBlogPostFromService() {
    BlogPost mockBlog = BlogPost.newBuilder()
        .id("1")
        .title("Test Blog 1")
        .content("Test Content 1")
        .tags(Arrays.asList("Tag1", "Tag2"))
        .build();

    when(blogGraphQLService.locateBlogPost("1")).thenReturn(mockBlog);
    BlogPost result = blogDataFetcher.locateBlogPost("1");
    assertThat(result).isEqualTo(mockBlog);

    assertThat(result.getTitle()).isEqualTo("Test Blog 1");
    assertThat(result.getContent()).isEqualTo("Test Content 1");
    assertThat(result.getTags()).containsExactly("Tag1", "Tag2");
  }
}
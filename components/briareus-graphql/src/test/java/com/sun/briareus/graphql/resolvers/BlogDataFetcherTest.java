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
import com.sun.briareus.codegen.types.BlogPostInput;
import com.sun.briareus.codegen.types.BlogPostType;
import com.sun.briareus.codegen.types.PagedBlogPosts;
import com.sun.briareus.codegen.types.PageInfo;
import com.sun.briareus.codegen.types.PaginationInput;
import com.sun.briareus.codegen.types.QueryResult;
import com.sun.briareus.codegen.types.QuerySuccess;

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
  void listBlogPosts_shouldReturnPageFromService() {
    PagedBlogPosts mockPage = PagedBlogPosts.newBuilder()
        .items(mockBlogPosts)
        .pageInfo(PageInfo.newBuilder()
            .page(0).size(10).totalPages(1).totalCount(2)
            .hasNextPage(false).hasPreviousPage(false).build())
        .build();
    PaginationInput pagination = PaginationInput.newBuilder().build();
    when(blogGraphQLService.listBlogPosts(pagination)).thenReturn(mockPage);

    PagedBlogPosts result = blogDataFetcher.listBlogPosts(pagination);

    assertThat(result).isEqualTo(mockPage);
    assertThat(result.getItems()).hasSize(2);
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
  }

  @Test
  void blogPostTypes_shouldReturnTypesFromService() {
    List<BlogPostType> types = Arrays.asList(
        BlogPostType.newBuilder().id("1").name("BOT_FAQ").build());
    when(blogGraphQLService.blogPostTypes()).thenReturn(types);

    assertThat(blogDataFetcher.blogPostTypes()).isEqualTo(types);
  }

  @Test
  void createBlogPost_shouldReturnQueryResultFromService() {
    BlogPostInput input = BlogPostInput.newBuilder()
        .content("New content")
        .tags(Arrays.asList("new", "tag"))
        .build();

    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").build();

    when(blogGraphQLService.createBlogPost("New Title", input)).thenReturn(mockResult);

    QueryResult result = blogDataFetcher.createBlogPost("New Title", input);

    assertThat(result).isEqualTo(mockResult);
  }

  @Test
  void createBlogPostType_shouldReturnQueryResultFromService() {
    QueryResult mockResult = QuerySuccess.newBuilder().message("ok").build();
    when(blogGraphQLService.createBlogPostType("BOT_FAQ", null)).thenReturn(mockResult);

    QueryResult result = blogDataFetcher.createBlogPostType("BOT_FAQ", null);

    assertThat(result).isEqualTo(mockResult);
  }
}

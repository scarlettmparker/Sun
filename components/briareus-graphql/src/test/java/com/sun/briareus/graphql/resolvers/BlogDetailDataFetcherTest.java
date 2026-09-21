package com.sun.briareus.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.briareus.codegen.types.BlogDetail;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogWithPropertiesInput;
import com.sun.briareus.codegen.types.CreateBlogWithPropertiesResponse;
import com.sun.briareus.codegen.types.UpdateBlogWithPropertiesResponse;
import com.sun.briareus.graphql.services.BlogDetailGqlService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlogDetailDataFetcherTest {

  @Mock
  private BlogDetailGqlService service;

  @InjectMocks
  private BlogDetailDataFetcher fetcher;

  @Test
  void blogDetail_shouldDelegate() {
    BlogDetail detail = BlogDetail.newBuilder()
        .post(BlogPost.newBuilder().id("1").title("t").build())
        .galleryItems(List.of())
        .attachedTexts(List.of())
        .linkedPosts(List.of())
        .build();
    when(service.blogDetail("1")).thenReturn(detail);

    assertThat(fetcher.blogDetail("1")).isEqualTo(detail);
  }

  @Test
  void createBlogWithProperties_shouldDelegate() {
    BlogWithPropertiesInput input = BlogWithPropertiesInput.newBuilder().title("t").content("c").typeId("tid").build();
    CreateBlogWithPropertiesResponse response = CreateBlogWithPropertiesResponse.newBuilder()
        .message("Created")
        .post(BlogPost.newBuilder().id("1").title("t").build())
        .build();
    when(service.createBlogWithProperties(input)).thenReturn(response);

    assertThat(fetcher.createBlogWithProperties(input)).isEqualTo(response);
  }

  @Test
  void updateBlogWithProperties_shouldDelegate() {
    BlogWithPropertiesInput input = BlogWithPropertiesInput.newBuilder().title("t").content("c").typeId("tid").build();
    UpdateBlogWithPropertiesResponse response = UpdateBlogWithPropertiesResponse.newBuilder()
        .message("Updated")
        .post(BlogPost.newBuilder().id("1").title("t").build())
        .build();
    when(service.updateBlogWithProperties("1", input)).thenReturn(response);

    assertThat(fetcher.updateBlogWithProperties("1", input)).isEqualTo(response);
  }
}

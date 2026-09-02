package com.sun.briareus.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sun.briareus.codegen.types.BlogDetail;
import com.sun.briareus.codegen.types.BlogWithPropertiesInput;
import com.sun.briareus.codegen.types.QuerySuccess;
import com.sun.briareus.graphql.services.BlogDetailGqlService;
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
        .post(com.sun.briareus.codegen.types.BlogPost.newBuilder().id("1").title("t").build())
        .galleryItems(java.util.List.of())
        .attachedTexts(java.util.List.of())
        .linkedPosts(java.util.List.of())
        .build();
    when(service.blogDetail("1")).thenReturn(detail);

    assertThat(fetcher.blogDetail("1")).isEqualTo(detail);
  }

  @Test
  void createBlogWithProperties_shouldDelegate() {
    BlogWithPropertiesInput input = BlogWithPropertiesInput.newBuilder().title("t").content("c").typeId("tid").build();
    QuerySuccess success = QuerySuccess.newBuilder().message("Created").id("1").build();
    when(service.createBlogWithProperties(input)).thenReturn(success);

    assertThat(fetcher.createBlogWithProperties(input)).isEqualTo(success);
  }

  @Test
  void updateBlogWithProperties_shouldDelegate() {
    BlogWithPropertiesInput input = BlogWithPropertiesInput.newBuilder().title("t").content("c").typeId("tid").build();
    QuerySuccess success = QuerySuccess.newBuilder().message("Updated").id("1").build();
    when(service.updateBlogWithProperties("1", input)).thenReturn(success);

    assertThat(fetcher.updateBlogWithProperties("1", input)).isEqualTo(success);
  }
}

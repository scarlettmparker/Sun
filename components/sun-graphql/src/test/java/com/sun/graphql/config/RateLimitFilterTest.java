package com.sun.graphql.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  private static final String CREATE_ANNOTATION_BODY = """
      {"query":"mutation createAnnotation($input: AnnotationInput!) { hadesMutations { createAnnotation(input: $input) { ... on QuerySuccess { message } } } }"}
      """;
  private static final String EDIT_ANNOTATION_BODY = """
      {"query":"mutation editAnnotation($id: ID!, $body: String!) { hadesMutations { editAnnotation(id: $id, body: $body) { ... on QuerySuccess { message } } } }"}
      """;
  private static final String QUERY_BODY = """
      {"query":"query texts { hadesQueries { texts { items { id } } } }"}
      """;

  private RateLimitFilter filter;
  private RateLimitRegistry registry;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    RateLimitProperties properties = new RateLimitProperties(true, 2, 0);
    registry = new RateLimitRegistry(Mockito.mock(ApplicationContext.class));
    filter = new RateLimitFilter(properties, registry, 1048576L);
    chain = Mockito.mock(FilterChain.class);
  }

  private MockHttpServletRequest post(String uri, String body) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
    request.setContent(body.getBytes());
    return request;
  }

  @Test
  void allowsRequestsWithinDefaultBucket() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(post("/graphql", QUERY_BODY), response, chain);
    filter.doFilter(post("/graphql", QUERY_BODY), response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(chain, Mockito.times(2)).doFilter(Mockito.any(), Mockito.any());
  }

  @Test
  void rejectsRequestsOverDefaultBucketWith429() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(post("/graphql", QUERY_BODY), response, chain);
    filter.doFilter(post("/graphql", QUERY_BODY), response, chain);
    filter.doFilter(post("/graphql", QUERY_BODY), response, chain);

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isNotBlank();
    verify(chain, Mockito.times(2)).doFilter(Mockito.any(), Mockito.any());
  }

  @Test
  void annotatedOperationGetsItsOwnBucket() throws Exception {
    registry.register("createAnnotation",
        new RateLimitBinding("createAnnotation", new RateLimitConfig(1, 0)));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(post("/graphql", CREATE_ANNOTATION_BODY), response, chain);
    filter.doFilter(post("/graphql", QUERY_BODY), response, chain);
    filter.doFilter(post("/graphql", CREATE_ANNOTATION_BODY), response, chain);

    assertThat(response.getStatus()).isEqualTo(429);
    verify(chain, Mockito.times(2)).doFilter(Mockito.any(), Mockito.any());
  }

  @Test
  void globPatternCoversMatchingOperations() throws Exception {
    registry.register("*Annotation",
        new RateLimitBinding("*Annotation", new RateLimitConfig(1, 0)));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(post("/graphql", CREATE_ANNOTATION_BODY), response, chain);
    filter.doFilter(post("/graphql", EDIT_ANNOTATION_BODY), response, chain);

    assertThat(response.getStatus()).isEqualTo(429);
    verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
  }

  @Test
  void ignoresNonGraphqlPaths() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(post("/something-else", QUERY_BODY), response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
  }

  @Test
  void ignoresGetRequests() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/graphql");
    request.setContent(QUERY_BODY.getBytes());

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
  }

  @Test
  void unparseableBodyFallsBackToDefaultBucket() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(post("/graphql", "not-json"), response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
  }
}

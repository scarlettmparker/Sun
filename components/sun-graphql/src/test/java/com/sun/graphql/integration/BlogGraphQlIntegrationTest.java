package com.sun.graphql.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.graphql.support.AbstractPostgresIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class BlogGraphQlIntegrationTest extends AbstractPostgresIntegrationTest {

  @Autowired private TestRestTemplate rest;

  @Test
  @DisplayName("blogPostTypes returns the seeded post types")
  void blogPostTypesReturnsSeededTypes() {
    JsonNode body = query("{ blogQueries { blogPostTypes { name } } }");

    assertThat(body.has("errors")).isFalse();
    assertThat(body.at("/data/blogQueries/blogPostTypes").isArray()).isTrue();
    assertThat(body.at("/data/blogQueries/blogPostTypes").size()).isGreaterThan(0);
  }

  @Test
  @DisplayName("hades sources query is wired and returns a list")
  void hadesSourcesIsWired() {
    JsonNode body = query("{ hadesQueries { sources { id name } } }");

    assertThat(body.has("errors")).isFalse();
    assertThat(body.at("/data/hadesQueries/sources").isArray()).isTrue();
  }

  @Test
  @DisplayName("the SDL endpoint serves the merged schema")
  void schemaEndpointServesSdl() {
    ResponseEntity<String> response = rest.getForEntity("/graphql/schema", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).contains("type Query");
  }

  private JsonNode query(String graphQl) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<JsonNode> response =
        rest.postForEntity("/graphql", new HttpEntity<>(Map.of("query", graphQl), headers), JsonNode.class);
    return response.getBody();
  }
}

package com.sun.graphql.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ClientSecretFilterTest {

  private FilterChain chain;

  @BeforeEach
  void setUp() {
    chain = Mockito.mock(FilterChain.class);
  }

  private ClientSecretFilter filter() {
    return new ClientSecretFilter(new ClientSecretProperties(Map.of("sun", "s3cret")));
  }

  @Test
  void validCredentials_continueTheChain() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/graphql");
    request.addHeader("X-Client-Id", "sun");
    request.addHeader("X-Client-Secret", "s3cret");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter().doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(chain).doFilter(request, response);
  }

  @Test
  void missingCredentials_areRejected() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/graphql");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter().doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(401);
    verifyNoInteractions(chain);
  }

  @Test
  void schemaEndpoint_isExemptWithoutCredentials() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/graphql/schema");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter().doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(chain).doFilter(request, response);
  }
}

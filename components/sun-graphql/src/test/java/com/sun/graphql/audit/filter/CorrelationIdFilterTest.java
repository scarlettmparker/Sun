package com.sun.graphql.audit.filter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void generatesIdWhenNoHeaderPresent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, Mockito.mock(FilterChain.class));

    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isNotBlank();
    assertThat(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE))
        .isEqualTo(response.getHeader(CorrelationIdFilter.HEADER));
  }

  @Test
  void echoesProvidedCorrelationId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.HEADER, "abc-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, Mockito.mock(FilterChain.class));

    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc-123");
  }

  @Test
  void fallsBackToAltHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CorrelationIdFilter.ALT_HEADER, "req-9");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, Mockito.mock(FilterChain.class));

    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("req-9");
  }
}

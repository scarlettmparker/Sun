package com.sun.graphql.audit.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.base.audit.context.AuditContext;
import com.sun.base.audit.context.AuditRequestSnapshot;
import com.sun.graphql.audit.graphql.AuditOperationRegistry;
import com.sun.graphql.audit.service.AuditEventService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuditFilterTest {

  private AuditContext auditContext;
  private AuditEventService auditEventService;
  private AuditFilter filter;

  @BeforeEach
  void setUp() {
    auditContext = new AuditContext();
    auditEventService = Mockito.mock(AuditEventService.class);
    filter = new AuditFilter(
        auditContext, auditEventService, new AuditOperationRegistry(), new ObjectMapper());
  }

  @AfterEach
  void tearDown() {
    auditContext.clear();
  }

  @Test
  void recordsFallbackOperationForGraphqlRequestWithNoOperations() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/graphql");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, Mockito.mock(FilterChain.class));

    ArgumentCaptor<AuditRequestSnapshot> captor = ArgumentCaptor.forClass(AuditRequestSnapshot.class);
    verify(auditEventService).persist(captor.capture(), anyLong());
    AuditRequestSnapshot snapshot = captor.getValue();
    assertThat(snapshot.operations()).hasSize(1);
    assertThat(snapshot.operations().get(0).operationName()).isEqualTo("unknown");
    assertThat(snapshot.endpoint()).isEqualTo("/graphql");
  }

  @Test
  void recordsSyntheticOperationForRestEndpoint() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, Mockito.mock(FilterChain.class));

    ArgumentCaptor<AuditRequestSnapshot> captor = ArgumentCaptor.forClass(AuditRequestSnapshot.class);
    verify(auditEventService).persist(captor.capture(), anyLong());
    assertThat(captor.getValue().operations().get(0).operationName()).isEqualTo("/auth/login");
  }

  @Test
  void skipsOptionsPreflight() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/graphql");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, Mockito.mock(FilterChain.class));

    verify(auditEventService, Mockito.never()).persist(any(), anyLong());
  }

  @Test
  void persistsEvenWhenDownstreamChainThrows() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/graphql");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = Mockito.mock(FilterChain.class);
    Mockito.doThrow(new RuntimeException("downstream boom"))
        .when(chain).doFilter(any(), any());

    // The downstream exception propagates; the finally block still persists.
    assertThatThrownBy(() -> filter.doFilter(request, response, chain))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("downstream boom");

    verify(auditEventService).persist(any(), anyLong());
  }
}

package com.sun.graphql.audit.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.base.audit.context.AuditContext;
import com.sun.gaia.service.UserContextHolder;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuditIdentityFilterTest {

  private final AuditContext auditContext = new AuditContext();
  private final AuditIdentityFilter filter = new AuditIdentityFilter(auditContext);

  @AfterEach
  void tearDown() {
    UserContextHolder.clear();
    auditContext.clear();
  }

  @Test
  void copiesLoggedInUserIdIntoContext() throws Exception {
    auditContext.begin();
    UUID userId = UUID.randomUUID();
    UserContextHolder.setUserId(userId);

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), Mockito.mock(FilterChain.class));

    assertThat(auditContext.getUserId()).isEqualTo(userId);
  }

  @Test
  void recordsNullWhenAnonymous() throws Exception {
    auditContext.begin();

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), Mockito.mock(FilterChain.class));

    assertThat(auditContext.getUserId()).isNull();
  }
}

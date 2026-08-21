package com.sun.narcissus.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.narcissus.codegen.types.VncCredentials;
import com.sun.narcissus.graphql.services.ViewerGraphQLService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ViewerDataFetcherTest {

  @Mock private ViewerGraphQLService viewerGraphQLService;
  @InjectMocks private ViewerDataFetcher fetcher;

  @Test
  void vncCredentials_shouldDelegateToService() {
    VncCredentials creds = VncCredentials.newBuilder().iframeSrc("/novnc/vnc.html?token=xyz").build();
    when(viewerGraphQLService.vncCredentials()).thenReturn(creds);

    VncCredentials result = fetcher.vncCredentials();

    assertThat(result).isEqualTo(creds);
    assertThat(result.getIframeSrc()).isEqualTo("/novnc/vnc.html?token=xyz");
    verify(viewerGraphQLService).vncCredentials();
  }

  @Test
  void getViewerQueries_shouldReturnInstance() {
    var result = fetcher.getViewerQueries();

    assertThat(result).isNotNull();
  }
}

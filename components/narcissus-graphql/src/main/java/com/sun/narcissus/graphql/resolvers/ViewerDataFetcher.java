package com.sun.narcissus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.narcissus.codegen.types.ViewerQueries;
import com.sun.narcissus.codegen.types.VncCredentials;
import com.sun.narcissus.service.VncCredentialsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

@DgsComponent
public class ViewerDataFetcher {

  private static final Logger log = LoggerFactory.getLogger(ViewerDataFetcher.class);

  private final VncCredentialsService credentialsService;

  public ViewerDataFetcher(VncCredentialsService credentialsService) {
    this.credentialsService = credentialsService;
  }

  @DgsData(parentType = "Query", field = "viewerQueries")
  public ViewerQueries getViewerQueries() {
    return ViewerQueries.newBuilder().build();
  }

  @DgsData(parentType = "ViewerQueries", field = "vncCredentials")
  @PreAuthorize("@permissions.has('graphql.narcissus.vncCredentials')")
  public VncCredentials vncCredentials() {
    try {
      String iframeSrc = credentialsService.generateIframeSrc();
      return VncCredentials.newBuilder().iframeSrc(iframeSrc).build();
    } catch (Exception e) {
      log.error("Failed to generate VNC credentials", e);
      throw new RuntimeException("Failed to generate VNC credentials", e);
    }
  }
}

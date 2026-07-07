package com.sun.narcissus.graphql.services;

import com.sun.narcissus.codegen.types.VncCredentials;
import com.sun.narcissus.service.VncCredentialsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * GraphQL business logic for the viewer.
 */
@Service
public class ViewerGraphQLService {

  private static final Logger log = LoggerFactory.getLogger(ViewerGraphQLService.class);

  private final VncCredentialsService credentialsService;

  public ViewerGraphQLService(VncCredentialsService credentialsService) {
    this.credentialsService = credentialsService;
  }

  /**
   * Returns the noVNC iframe src with a single-use token.
   *
   * @return the VNC credentials
   */
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

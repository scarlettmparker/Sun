package com.sun.graphql.config;

import com.sun.base.permify.PermifyProps;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Pushes the Permify schema on backend startup.
 */
@Component
public class PermifySchemaInitializer {

  private static final Logger logger = LoggerFactory.getLogger(PermifySchemaInitializer.class);

  private final PermifyProps props;
  private final RestClient restClient;

  public PermifySchemaInitializer(PermifyProps props) {
    this.props = props;
    this.restClient = RestClient.builder().baseUrl(props.getHttpEndpoint()).build();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    if (!props.isEnabled()) {
      return;
    }
    try {
      String schema = null;
      ClassPathResource resource = new ClassPathResource("permify/schema.zed");
      if (resource.exists()) {
        schema = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      } else {
        Path path = Paths.get("permify/schema.zed");
        if (Files.exists(path)) {
          schema = Files.readString(path, StandardCharsets.UTF_8);
        } else {
          Path alt = Paths.get(System.getProperty("user.dir"), "permify/schema.zed");
          if (Files.exists(alt)) {
            schema = Files.readString(alt, StandardCharsets.UTF_8);
          }
        }
      }
      if (schema == null || schema.isBlank()) {
        logger.warn("Permify schema not found on classpath or filesystem");
        return;
      }
      Map<String, String> body = Map.of("schema", schema);
      restClient.post()
          .uri("/v1/tenants/t1/schemas/write")
          .body(body)
          .retrieve()
          .toBodilessEntity();
      logger.info("Permify schema pushed on startup");
    } catch (Exception e) {
      logger.warn("Failed to push Permify schema on startup", e);
    }
  }
}

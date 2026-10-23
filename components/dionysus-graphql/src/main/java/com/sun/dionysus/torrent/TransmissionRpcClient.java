package com.sun.dionysus.torrent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sends JSON-RPC requests to transmission-daemon.
 */
@Component
public class TransmissionRpcClient {

  private static final Logger log = LoggerFactory.getLogger(TransmissionRpcClient.class);

  private final ObjectMapper mapper = new ObjectMapper();

  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Value("${dionysus.transmission.rpc-url:http://127.0.0.1:9091/transmission/rpc}")
  private String rpcUrl;

  @Value("${dionysus.transmission.username:transmission}")
  private String username;

  @Value("${dionysus.transmission.password:transmission}")
  private String password;

  private volatile String sessionId;

  /**
   * Executes a transmission RPC method.
   *
   * @param method the transmission method name.
   * @param arguments the method arguments.
   * @return the response arguments, or null when the call fails.
   */
  public JsonNode call(String method, ObjectNode arguments) {
    ObjectNode payload = mapper.createObjectNode();
    payload.put("method", method);
    payload.set("arguments", arguments == null ? mapper.createObjectNode() : arguments);

    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        HttpResponse<String> response =
            http.send(request(payload), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 409) {
          sessionId = response.headers().firstValue("X-Transmission-Session-Id").orElse(null);
          continue;
        }
        if (response.statusCode() == 401) {
          log.error("Transmission RPC rejected the configured credentials");
          return null;
        }
        if (response.statusCode() != 200) {
          log.warn("Transmission RPC {} returned HTTP {}", method, response.statusCode());
          return null;
        }

        JsonNode root = mapper.readTree(response.body());
        if (!"success".equals(root.path("result").asText())) {
          log.warn("Transmission RPC {} failed: {}", method, root.path("result").asText());
          return null;
        }
        return root.path("arguments");
      } catch (Exception e) {
        log.warn("Transmission RPC {} failed: {}", method, e.getMessage());
        return null;
      }
    }

    log.warn("Transmission RPC {} could not obtain a session id", method);
    return null;
  }

  /**
   * Builds an authenticated RPC request.
   *
   * @param payload the JSON-RPC payload.
   * @return the HTTP request.
   * @throws Exception when the payload cannot be serialized.
   */
  private HttpRequest request(ObjectNode payload) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder()
            .uri(URI.create(rpcUrl))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("Authorization", basicAuth())
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)));

    if (sessionId != null) {
      builder.header("X-Transmission-Session-Id", sessionId);
    }
    return builder.build();
  }

  /**
   * Encodes the configured credentials for HTTP basic auth.
   *
   * @return the Authorization header value.
   */
  private String basicAuth() {
    String credentials = username + ":" + password;
    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}

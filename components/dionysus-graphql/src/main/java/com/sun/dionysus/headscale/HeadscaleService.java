package com.sun.dionysus.headscale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Communicates with the Headscale REST API to manage Tailscale nodes and
 * pre-auth keys.
 */
@Component
public class HeadscaleService {

  private static final Logger log = LoggerFactory.getLogger(HeadscaleService.class);

  private final String baseUrl;
  private final String apiKey;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public HeadscaleService(
      @Value("${HEADSCALE_URL:https://vps.scarlettparker.co.uk}") String baseUrl,
      @Value("${HEADSCALE_API_KEY:}") String apiKey) {
    this.baseUrl = baseUrl.replaceAll("/$", "");
    this.apiKey = apiKey;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * A node on the Tailscale network.
   *
   * @param id       the Headscale node id.
   * @param name     the node hostname.
   * @param ipv4     the Tailscale IPv4 address (100.x.x.x).
   * @param online   whether the node is currently connected.
   * @param lastSeen ISO-8601 timestamp of last contact.
   */
  public record HeadscaleNode(long id, String name, String ipv4, boolean online, String lastSeen) {}

  /**
   * Creates a pre-auth key valid for the given duration.
   *
   * @return the pre-auth key string (hskey-auth-...).
   */
  public String createPreAuthKey(Duration expiry) {
    try {
      String expiration = Instant.now().plus(expiry).toString();
      String body = objectMapper.writeValueAsString(
          new PreAuthKeyRequest(1, false, false, expiration));

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/v1/preauthkey"))
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .timeout(Duration.ofSeconds(15))
          .build();

      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("Headscale preauthkey failed: " + response.statusCode()
            + " " + response.body());
      }

      JsonNode root = objectMapper.readTree(response.body());
      return root.path("preAuthKey").path("key").asText();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create Headscale pre-auth key", e);
    }
  }

  /**
   * Lists all nodes registered on the tailnet.
   */
  public List<HeadscaleNode> listNodes() {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/v1/node"))
          .header("Authorization", "Bearer " + apiKey)
          .GET()
          .timeout(Duration.ofSeconds(15))
          .build();

      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("Headscale list nodes failed: " + response.statusCode());
      }

      JsonNode root = objectMapper.readTree(response.body());
      List<HeadscaleNode> nodes = new ArrayList<>();
      for (JsonNode n : root.path("nodes")) {
        String ipv4 = "";
        for (JsonNode ip : n.path("ipAddresses")) {
          String addr = ip.asText();
          if (addr.startsWith("100.")) {
            ipv4 = addr;
            break;
          }
        }
        nodes.add(new HeadscaleNode(
            n.path("id").asLong(),
            n.path("givenName").asText(""),
            ipv4,
            n.path("online").asBoolean(false),
            n.path("lastSeen").asText("")));
      }
      return nodes;
    } catch (Exception e) {
      throw new RuntimeException("Failed to list Headscale nodes", e);
    }
  }

  /**
   * Expires a node, immediately removing it from the tailnet.
   */
  public void expireNode(long nodeId) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/api/v1/node/" + nodeId + "/expire"))
          .header("Authorization", "Bearer " + apiKey)
          .POST(HttpRequest.BodyPublishers.noBody())
          .timeout(Duration.ofSeconds(15))
          .build();

      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("Headscale expire node failed: " + response.statusCode());
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to expire Headscale node " + nodeId, e);
    }
  }

  /**
   * Request body for creating a pre-auth key.
   */
  private record PreAuthKeyRequest(int user, boolean reusable, boolean ephemeral, String expiration) {}
}

package com.sun.base.nlp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.sun.base.cache.CaffeineSpec;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fetches Wikipedia summaries via the public REST API.
 */
@Component
public class WikipediaService {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public WikipediaService() {
    this.restClient = RestClient.builder()
        .baseUrl("https://en.wikipedia.org")
        .defaultHeader("User-Agent", "SunKnowledge/1.0 (scarlett)")
        .build();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Fetches a cached Wikipedia summary.
   *
   * @param title the page title
   * @return the summary, or null when not found
   */
  @Cacheable(value = "wikipediaSummary", key = "#title.toLowerCase().trim()")
  @CaffeineSpec(expireAfterWrite = "24h", maximumSize = 1000)
  public WikipediaSummary summary(String title) {
    if (title == null || title.trim().isEmpty()) {
      return null;
    }
    String enc = encode(title.trim());
    try {
      String json = restClient.get()
          .uri("/api/rest_v1/page/summary/{enc}", enc)
          .retrieve()
          .body(String.class);
      return map(json);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Maps raw JSON to a summary.
   *
   * @param json the raw JSON
   * @return the summary or null
   */
  private WikipediaSummary map(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      JsonNode node = objectMapper.readTree(json);
      if (node.has("type") && node.get("type").asText("").contains("not_found")) {
        return null;
      }
      String extract = node.path("extract").asText(null);
      if (extract == null || extract.isBlank()) {
        return null;
      }
      String title = node.path("title").asText(null);
      String pageUrl = node.path("content_urls").path("desktop").path("page").asText(null);
      if (pageUrl == null || pageUrl.isBlank()) {
        String fallbackTitle = title == null ? "" : title;
        pageUrl = "https://en.wikipedia.org/wiki/" + encode(fallbackTitle);
      }
      String thumbnailUrl = node.path("thumbnail").path("source").asText(null);
      return new WikipediaSummary(title, extract, pageUrl, thumbnailUrl);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Encodes a value for a URL path.
   *
   * @param value the value to encode
   * @return the encoded value
   */
  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }
}

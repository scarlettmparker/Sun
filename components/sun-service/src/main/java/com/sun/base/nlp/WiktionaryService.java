package com.sun.base.nlp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.sun.base.cache.CaffeineSpec;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fetches English Wiktionary definitions.
 */
@Component
public class WiktionaryService {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public WiktionaryService() {
    this.restClient = RestClient.builder()
        .baseUrl("https://en.wiktionary.org")
        .defaultHeader("User-Agent", "SunKnowledge/1.0 (scarlett)")
        .build();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Fetches a cached English entry.
   *
   * @param word the headword
   * @return the entry, or null when not found
   */
  @Cacheable(value = "wiktionaryEntry", key = "#word.toLowerCase().trim()")
  @CaffeineSpec(expireAfterWrite = "24h", maximumSize = 1000)
  public WiktionaryEntry define(String word) {
    if (word == null || word.trim().isEmpty()) {
      return null;
    }
    String norm = word.trim();
    String enc = encode(norm);
    try {
      String json = restClient.get()
          .uri("/api/rest_v1/page/definition/{enc}", enc)
          .retrieve()
          .body(String.class);
      WiktionaryEntry fromJson = mapJson(json, norm);
      if (fromJson != null && !fromJson.definitions().isEmpty()) {
        return fromJson;
      }
    } catch (Exception ignored) {
      // fall through to HTML fallback
    }
    return fetchHtmlFallback(norm, enc);
  }

  /**
   * Maps JSON to an entry.
   *
   * @param json the raw JSON
   * @param word the headword
   * @return the entry or null
   */
  private WiktionaryEntry mapJson(String json, String word) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode en = root.path("en");
      if (!en.isArray() || en.isEmpty()) {
        return null;
      }
      List<String> defs = new ArrayList<>();
      for (JsonNode sense : en) {
        JsonNode definitions = sense.path("definitions");
        if (definitions.isArray()) {
          for (JsonNode d : definitions) {
            String text = d.path("definition").asText(null);
            if (text == null || text.isBlank()) {
              text = d.asText(null);
            }
            if (text != null && !text.isBlank()) {
              defs.add(Jsoup.parse(text).text().trim());
            }
          }
        }
      }
      if (defs.isEmpty()) {
        return null;
      }
      String sourceUrl = "https://en.wiktionary.org/wiki/" + encode(word);
      return new WiktionaryEntry(word, List.copyOf(defs), sourceUrl);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Fetches HTML fallback for a word.
   *
   * @param word the headword
   * @param enc the encoded word
   * @return the entry or null
   */
  private WiktionaryEntry fetchHtmlFallback(String word, String enc) {
    try {
      String html = restClient.get()
          .uri("/wiki/{enc}", enc)
          .retrieve()
          .body(String.class);
      if (html == null || html.isBlank()) {
        return null;
      }
      Document doc = Jsoup.parse(html);
      Element english = doc.getElementById("English");
      if (english == null) {
        return null;
      }
      Element heading = english.closest("h2");
      if (heading == null) {
        heading = english.parent();
      }
      List<String> defs = new ArrayList<>();
      Element cur = heading == null ? null : heading.nextElementSibling();
      while (cur != null) {
        String tag = cur.tagName();
        if (tag.equals("h2")) {
          break;
        }
        if (tag.equals("ol")) {
          for (Element li : cur.select("li")) {
            String t = li.text().trim();
            if (!t.isEmpty()) {
              defs.add(t);
            }
          }
          if (!defs.isEmpty()) {
            break;
          }
        }
        cur = cur.nextElementSibling();
      }
      if (defs.isEmpty()) {
        return null;
      }
      String sourceUrl = "https://en.wiktionary.org/wiki/" + enc;
      return new WiktionaryEntry(word, List.copyOf(defs), sourceUrl);
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

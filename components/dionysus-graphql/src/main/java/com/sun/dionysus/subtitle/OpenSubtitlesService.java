package com.sun.dionysus.subtitle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sun.dionysus.model.enums.SubtitleSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * Client for the OpenSubtitles REST API.
 */
@Component
public class OpenSubtitlesService {

  private static final Logger logger = LoggerFactory.getLogger(OpenSubtitlesService.class);
  private static final String DEFAULT_BASE_URL = "https://api.opensubtitles.com/api/v1";

  private final String apiKey;
  private final String username;
  private final String password;
  private final String userAgent;
  private final String baseUrl;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final Cache<String, List<SubtitleSearchResult>> cache;

  private volatile String token;
  private final Object tokenLock = new Object();

  /**
   * Creates a service with a 24 hour, 500 entry search cache.
   *
   * @param apiKey the OpenSubtitles API key
   * @param username the OpenSubtitles username
   * @param password the OpenSubtitles password
   * @param userAgent the required User-Agent header
   */
  @Autowired
  public OpenSubtitlesService(
      @Value("${subtitle.opensubtitles-api-key:}") String apiKey,
      @Value("${subtitle.opensubtitles-username:}") String username,
      @Value("${subtitle.opensubtitles-password:}") String password,
      @Value("${subtitle.opensubtitles-user-agent:Filestore/1.0}") String userAgent) {
    this(apiKey, username, password, userAgent, Duration.ofHours(24), 500);
  }

  /**
   * Creates a service with a custom HTTP client.
   *
   * @param apiKey the OpenSubtitles API key
   * @param username the OpenSubtitles username
   * @param password the OpenSubtitles password
   * @param userAgent the required User-Agent header
   * @param httpClient the HTTP client to use
   */
  public OpenSubtitlesService(
      String apiKey, String username, String password, String userAgent, HttpClient httpClient) {
    this(apiKey, username, password, userAgent, Duration.ofHours(24), 500, httpClient);
  }

  /**
   * Creates a service with a custom cache lifetime.
   *
   * @param apiKey the OpenSubtitles API key
   * @param username the OpenSubtitles username
   * @param password the OpenSubtitles password
   * @param userAgent the required User-Agent header
   * @param cacheTtl the search cache time-to-live
   * @param cacheMaxSize the maximum number of cached searches
   */
  public OpenSubtitlesService(
      String apiKey,
      String username,
      String password,
      String userAgent,
      Duration cacheTtl,
      long cacheMaxSize) {
    this(apiKey, username, password, userAgent, cacheTtl, cacheMaxSize,
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }

  /**
   * Creates a fully configured service.
   *
   * @param apiKey the OpenSubtitles API key
   * @param username the OpenSubtitles username
   * @param password the OpenSubtitles password
   * @param userAgent the required User-Agent header
   * @param cacheTtl the search cache time-to-live
   * @param cacheMaxSize the maximum number of cached searches
   * @param httpClient the HTTP client to use
   */
  public OpenSubtitlesService(
      String apiKey,
      String username,
      String password,
      String userAgent,
      Duration cacheTtl,
      long cacheMaxSize,
      HttpClient httpClient) {
    this.apiKey = apiKey == null ? "" : apiKey;
    this.username = username == null ? "" : username;
    this.password = password == null ? "" : password;
    this.userAgent = userAgent == null || userAgent.isBlank() ? "Filestore/1.0" : userAgent;
    this.baseUrl = DEFAULT_BASE_URL;
    this.httpClient = httpClient;
    this.objectMapper = new ObjectMapper();
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(cacheTtl == null ? Duration.ofHours(24) : cacheTtl)
        .maximumSize(cacheMaxSize <= 0 ? 500 : cacheMaxSize)
        .build();
  }

  /**
   * Returns the search cache.
   *
   * @return the cache
   */
  public Cache<String, List<SubtitleSearchResult>> getCache() {
    return cache;
  }

  /**
   * Reports whether the service is configured.
   *
   * @return true when an API key and credentials are present
   */
  public boolean isEnabled() {
    return !apiKey.isBlank() && !username.isBlank() && !password.isBlank();
  }

  /**
   * Searches OpenSubtitles, returning cached results when available.
   *
   * @param query the movie or series query
   * @param year the release year, or null
   * @param language the subtitle language
   * @param movieHash the OpenSubtitles movie hash, or null
   * @param movieBytes the movie size in bytes, or null
   * @return the matching subtitles, or an empty list when disabled or on error
   */
  public List<SubtitleSearchResult> search(
      String query, Integer year, String language, String movieHash, Long movieBytes) {
    if (!isEnabled()) {
      return List.of();
    }

    String normalizedLanguage = normalizeLanguage(language);
    String normalizedHash = movieHash == null ? "" : movieHash;
    String normalizedQuery = query == null ? "" : query;
    String yearText = year == null ? "" : String.valueOf(year);
    String cacheKey = normalizedLanguage + "|" + normalizedHash + "|" + normalizedQuery + "|" + yearText;

    List<SubtitleSearchResult> cached = cache.getIfPresent(cacheKey);
    if (cached != null) {
      return new ArrayList<>(cached);
    }

    try {
      List<SubtitleSearchResult> results = searchRemote(
          normalizedQuery, year, normalizedLanguage, normalizedHash, movieBytes);
      cache.put(cacheKey, List.copyOf(results));
      return new ArrayList<>(results);
    } catch (Exception e) {
      logger.warn("OpenSubtitles search failed for '{}': {}", normalizedQuery, e.getMessage());
      return List.of();
    }
  }

  /**
   * Downloads a subtitle and returns its raw bytes.
   *
   * @param fileId the OpenSubtitles file id
   * @return the subtitle bytes, or null when disabled or on error
   */
  public byte[] download(int fileId) {
    if (!isEnabled()) {
      return null;
    }

    try {
      String currentToken = ensureToken();
      return downloadRemote(fileId, currentToken);
    } catch (Exception e) {
      logger.warn("OpenSubtitles download failed for file {}: {}", fileId, e.getMessage());
      return null;
    }
  }

  /**
   * Performs the remote search, refreshing the token on a 401.
   *
   * @param query the movie or series query
   * @param year the release year, or null
   * @param language the subtitle language
   * @param movieHash the OpenSubtitles movie hash
   * @param movieBytes the movie size in bytes, or null
   * @return the matching subtitles
   * @throws IOException when the request fails
   * @throws InterruptedException when the request is interrupted
   */
  protected List<SubtitleSearchResult> searchRemote(
      String query, Integer year, String language, String movieHash, Long movieBytes)
      throws IOException, InterruptedException {
    String currentToken = ensureToken();
    HttpRequest request = buildSearchRequest(query, year, language, movieHash, movieBytes, currentToken);
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() == 401) {
      token = null;
      currentToken = ensureToken();
      request = buildSearchRequest(query, year, language, movieHash, movieBytes, currentToken);
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    if (response.statusCode() != 200) {
      throw new IOException("OpenSubtitles search returned " + response.statusCode());
    }
    return parseSearchResponse(response.body());
  }

  /**
   * Performs the remote download, refreshing the token on a 401.
   *
   * @param fileId the OpenSubtitles file id
   * @param token the bearer token
   * @return the subtitle bytes
   * @throws IOException when the request fails
   * @throws InterruptedException when the request is interrupted
   */
  protected byte[] downloadRemote(int fileId, String token)
      throws IOException, InterruptedException {
    String currentToken = token;
    String body = objectMapper.writeValueAsString(Map.of("file_id", fileId, "sub_format", "srt"));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/download"))
        .header("Api-Key", apiKey)
        .header("User-Agent", userAgent)
        .header("Authorization", "Bearer " + currentToken)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .timeout(Duration.ofSeconds(30))
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 401) {
      this.token = null;
      currentToken = ensureToken();
      request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/download"))
          .header("Api-Key", apiKey)
          .header("User-Agent", userAgent)
          .header("Authorization", "Bearer " + currentToken)
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .timeout(Duration.ofSeconds(30))
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    if (response.statusCode() != 200) {
      throw new IOException("OpenSubtitles download returned " + response.statusCode());
    }

    JsonNode root = objectMapper.readTree(response.body());
    String link = root.path("link").asText("");
    if (link.isBlank()) {
      throw new IOException("OpenSubtitles download response had no link");
    }

    HttpRequest downloadRequest = HttpRequest.newBuilder()
        .uri(URI.create(link))
        .header("User-Agent", userAgent)
        .timeout(Duration.ofSeconds(60))
        .GET()
        .build();
    HttpResponse<byte[]> downloadResponse =
        httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
    if (downloadResponse.statusCode() != 200) {
      throw new IOException("OpenSubtitles file download returned " + downloadResponse.statusCode());
    }
    return unzipIfNeeded(downloadResponse.body());
  }

  /**
   * Ensures that a bearer token is available.
   *
   * @return the bearer token
   * @throws IOException when login fails
   * @throws InterruptedException when the request is interrupted
   */
  private String ensureToken() throws IOException, InterruptedException {
    String current = token;
    if (current != null && !current.isBlank()) {
      return current;
    }
    synchronized (tokenLock) {
      current = token;
      if (current != null && !current.isBlank()) {
        return current;
      }
      login();
      return token;
    }
  }

  /**
   * Logs in and caches the bearer token.
   *
   * @throws IOException when login fails
   * @throws InterruptedException when the request is interrupted
   */
  private void login() throws IOException, InterruptedException {
    String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/login"))
        .header("Api-Key", apiKey)
        .header("User-Agent", userAgent)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .timeout(Duration.ofSeconds(15))
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("OpenSubtitles login returned " + response.statusCode());
    }

    JsonNode root = objectMapper.readTree(response.body());
    String newToken = root.path("token").asText("");
    if (newToken.isBlank()) {
      throw new IOException("OpenSubtitles login response had no token");
    }
    token = newToken;
  }

  /**
   * Builds the search request.
   */
  private HttpRequest buildSearchRequest(
      String query, Integer year, String language, String movieHash, Long movieBytes, String authToken) {
    StringBuilder url = new StringBuilder(baseUrl).append("/subtitles?");
    appendParam(url, "query", query);
    if (year != null) {
      appendParam(url, "year", String.valueOf(year));
    }
    appendParam(url, "languages", language);
    if (movieHash != null && !movieHash.isBlank()) {
      appendParam(url, "moviehash", movieHash);
    }
    if (movieBytes != null) {
      appendParam(url, "moviebytes", String.valueOf(movieBytes));
    }

    return HttpRequest.newBuilder()
        .uri(URI.create(url.toString()))
        .header("Api-Key", apiKey)
        .header("User-Agent", userAgent)
        .header("Authorization", "Bearer " + authToken)
        .header("Accept", "application/json")
        .timeout(Duration.ofSeconds(20))
        .GET()
        .build();
  }

  /**
   * Appends a URL parameter.
   */
  private static void appendParam(StringBuilder url, String name, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    url.append(URLEncoder.encode(name, StandardCharsets.UTF_8))
        .append("=")
        .append(URLEncoder.encode(value, StandardCharsets.UTF_8))
        .append("&");
  }

  /**
   * Parses a search response.
   */
  private List<SubtitleSearchResult> parseSearchResponse(String body) throws IOException {
    List<SubtitleSearchResult> results = new ArrayList<>();
    JsonNode root = objectMapper.readTree(body);
    JsonNode data = root.path("data");
    if (!data.isArray()) {
      return results;
    }

    for (JsonNode item : data) {
      JsonNode attributes = item.path("attributes");
      JsonNode files = attributes.path("files");
      if (!files.isArray() || files.isEmpty()) {
        continue;
      }
      JsonNode file = files.get(0);
      Integer fileId = file.has("file_id") ? file.get("file_id").asInt() : null;
      String language = attributes.path("language").asText("");
      String label = attributes.path("release").asText("");
      if (label.isBlank()) {
        label = file.path("file_name").asText("");
      }
      Boolean hearingImpaired = attributes.has("hearing_impaired")
          ? attributes.get("hearing_impaired").asBoolean()
          : null;
      Integer downloads = attributes.has("download_count")
          ? attributes.get("download_count").asInt()
          : null;
      Double rating = attributes.has("ratings") && attributes.get("ratings").isNumber()
          ? attributes.get("ratings").asDouble()
          : null;

      results.add(new SubtitleSearchResult(
          SubtitleSource.OPENSUBTITLES,
          null,
          fileId,
          language.isBlank() ? "en" : language,
          label,
          hearingImpaired,
          downloads,
          rating));
    }
    return results;
  }

  /**
   * Unzips a payload when it is a zip archive.
   */
  private static byte[] unzipIfNeeded(byte[] data) throws IOException {
    if (data == null || data.length < 2 || data[0] != 0x50 || data[1] != 0x4b) {
      return data;
    }
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
      ZipEntry entry = zip.getNextEntry();
      if (entry == null) {
        return data;
      }
      return zip.readAllBytes();
    }
  }

  /**
   * Normalizes a language code.
   */
  private static String normalizeLanguage(String language) {
    if (language == null || language.isBlank()) {
      return "en";
    }
    return language.trim().toLowerCase(java.util.Locale.ROOT);
  }
}

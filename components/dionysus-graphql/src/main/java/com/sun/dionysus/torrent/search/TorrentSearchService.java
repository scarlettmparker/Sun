package com.sun.dionysus.torrent.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;

/**
 * Searches Prowlarr's search API for torrent results.
 */
@Component
public class TorrentSearchService {

    private static final Logger log = LoggerFactory.getLogger(TorrentSearchService.class);

    private static final List<String> CATEGORIES = List.of(
            "2000", "2010", "2020", "2030", "2040", "5000", "6000", "7000", "8000");

    private final String prowlarrUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TorrentSearchService(
            @Value("${torrent-search.prowlarr-url:}") String prowlarrUrl,
            @Value("${torrent-search.prowlarr-api-key:}") String apiKey) {
        this.prowlarrUrl = prowlarrUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Searches Prowlarr and returns parsed torrent results.
     *
     * @param query the search query.
     * @return list of search results, or empty when disabled or on error.
     */
    public List<TorrentSearchResult> search(String query) {
        if (prowlarrUrl == null || prowlarrUrl.isBlank()
                || apiKey == null || apiKey.isBlank()) {
            return List.of();
        }

        try {
            String url = buildUrl(query);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Api-Key", apiKey)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Prowlarr returned {} for search: {}", response.statusCode(), query);
                return List.of();
            }

            return parseResponse(response.body());
        } catch (Exception e) {
            log.error("Prowlarr search failed for query: {}", query, e);
            return List.of();
        }
    }

    /**
     * Builds the Prowlarr search API URL with the given query and categories.
     */
    private String buildUrl(String query) {
        StringBuilder url = new StringBuilder(prowlarrUrl.replaceAll("/$", ""))
                .append("/api/v1/search?query=")
                .append(URLEncoder.encode(query, StandardCharsets.UTF_8))
                .append("&type=search");
        for (String category : CATEGORIES) {
            url.append("&categories=").append(category);
        }
        return url.toString();
    }

    /**
     * Parses a Prowlarr JSON response into search results.
     */
    private List<TorrentSearchResult> parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            List<TorrentSearchResult> out = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode r : root) {
                    TorrentSearchResult item = parseItem(r);
                    if (item != null) {
                        out.add(item);
                    }
                }
            }

            if (out.isEmpty()) {
                log.info("Prowlarr returned no results - ensure at least one indexer "
                        + "is configured in the Prowlarr web UI (http://localhost:9696) "
                        + "and that it supports the selected categories");
            }

            return out;
        } catch (Exception e) {
            log.warn("Failed to parse Prowlarr response", e);
            return List.of();
        }
    }

    /**
     * Parses a single Prowlarr result item.
     *
     * @return the result, or null if the item has no magnet or download link.
     */
    private TorrentSearchResult parseItem(JsonNode item) {
        String title = item.path("title").asText("");
        String magnet = item.path("magnetUrl").asText("");
        String link = item.path("downloadUrl").asText("");
        if (magnet.isBlank()) {
            magnet = link;
        }
        if (magnet.isBlank()) {
            return null;
        }

        long rawSize = item.path("size").asLong(0);
        int seeders = item.path("seeders").asInt(0);
        int leechers = item.path("leechers").asInt(0);
        String size = formatSize(rawSize);
        String publishDate = item.path("publishDate").asText("");

        return new TorrentSearchResult(
                title,
                seeders,
                leechers,
                size,
                rawSize,
                publishDate,
                magnet);
    }

    /**
     * Converts bytes to a human-readable size string.
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String prefix = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), prefix);
    }
}

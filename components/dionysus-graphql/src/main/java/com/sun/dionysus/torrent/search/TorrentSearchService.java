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
 * Searches Jackett's internal JSON API for torrent results.
 */
@Component
public class TorrentSearchService {

    private static final Logger log = LoggerFactory.getLogger(TorrentSearchService.class);

    private static final String CATEGORIES = "2000,2010,2020,2030,2040,5000,6000,7000,8000";

    private final String jackettUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TorrentSearchService(
            @Value("${torrent-search.jackett-url:}") String jackettUrl,
            @Value("${torrent-search.jackett-api-key:}") String apiKey) {
        this.jackettUrl = jackettUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Searches Jackett and returns parsed torrent results.
     *
     * @param query the search query.
     * @return list of search results, or empty when disabled or on error.
     */
    public List<TorrentSearchResult> search(String query) {
        if (jackettUrl == null || jackettUrl.isBlank()
                || apiKey == null || apiKey.isBlank()) {
            return List.of();
        }

        try {
            String url = buildUrl(query);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Jackett returned {} for search: {}", response.statusCode(), query);
                return List.of();
            }

            return parseResponse(response.body());
        } catch (Exception e) {
            log.error("Jackett search failed for query: {}", query, e);
            return List.of();
        }
    }

    /**
     * Builds the Jackett JSON API URL with the given query and categories.
     */
    private String buildUrl(String query) {
        return String.format(
                "%s/api/v2.0/indexers/all/results?apikey=%s&Query=%s&Category=%s",
                jackettUrl.replaceAll("/$", ""),
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8),
                URLEncoder.encode(query, StandardCharsets.UTF_8),
                CATEGORIES);
    }

    /**
     * Parses a Jackett JSON response into search results.
     */
    private List<TorrentSearchResult> parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.path("Results");
            List<TorrentSearchResult> out = new ArrayList<>();

            if (results.isArray()) {
                for (JsonNode r : results) {
                    TorrentSearchResult item = parseItem(r);
                    if (item != null) {
                        out.add(item);
                    }
                }
            }

            if (out.isEmpty()) {
                log.info("Jackett returned no results — ensure at least one indexer "
                        + "is configured in the Jackett web UI (http://localhost:9117) "
                        + "and that it supports the selected categories");
            }

            return out;
        } catch (Exception e) {
            log.warn("Failed to parse Jackett response", e);
            return List.of();
        }
    }

    /**
     * Parses a single Jackett result item.
     *
     * @return the result, or null if the item has no magnet link.
     */
    private TorrentSearchResult parseItem(JsonNode item) {
        String title = item.path("Title").asText("");
        String magnet = item.path("MagnetUri").asText("");
        String link = item.path("Link").asText("");
        if (magnet.isBlank()) {
            magnet = link;
        }
        if (magnet.isBlank()) {
            return null;
        }

        long rawSize = item.path("Size").asLong(0);
        int seeders = item.path("Seeders").asInt(0);
        int peers = item.path("Peers").asInt(0);
        String size = formatSize(rawSize);
        String publishDate = item.path("PublishDate").asText("");

        return new TorrentSearchResult(
                title,
                seeders,
                Math.max(0, peers - seeders),
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

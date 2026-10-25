package com.sun.gaia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Exchanges a Google authorization code for the user's verified profile.
 */
@Service
public class GoogleOAuthService {

  private final String clientId;
  private final String clientSecret;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public GoogleOAuthService(
      @Value("${GOOGLE_CLIENT_ID:}") String clientId,
      @Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  public boolean isConfigured() {
    return clientId != null && !clientId.isBlank()
        && clientSecret != null && !clientSecret.isBlank();
  }

  public Optional<GoogleUser> exchangeCode(String code, String redirectUri) {
    if (!isConfigured()) {
      return Optional.empty();
    }
    try {
      String body = "code=" + enc(code)
          + "&client_id=" + enc(clientId)
          + "&client_secret=" + enc(clientSecret)
          + "&redirect_uri=" + enc(redirectUri)
          + "&grant_type=authorization_code";
      HttpRequest tokenReq = HttpRequest.newBuilder()
          .uri(URI.create("https://oauth2.googleapis.com/token"))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<String> tokenRes = httpClient.send(tokenReq, HttpResponse.BodyHandlers.ofString());
      if (tokenRes.statusCode() != 200) {
        return Optional.empty();
      }
      JsonNode tokenJson = objectMapper.readTree(tokenRes.body());
      String accessToken = tokenJson.path("access_token").asText(null);
      if (accessToken == null || accessToken.isBlank()) {
        return Optional.empty();
      }

      HttpRequest userReq = HttpRequest.newBuilder()
          .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
          .header("Authorization", "Bearer " + accessToken)
          .GET()
          .build();
      HttpResponse<String> userRes = httpClient.send(userReq, HttpResponse.BodyHandlers.ofString());
      if (userRes.statusCode() != 200) {
        return Optional.empty();
      }
      JsonNode userJson = objectMapper.readTree(userRes.body());
      String sub = userJson.path("sub").asText(null);
      String email = userJson.path("email").asText(null);
      boolean verified = userJson.path("email_verified").asBoolean(false);
      String name = userJson.path("name").asText(email);
      if (sub == null || email == null || !verified) {
        return Optional.empty();
      }
      return Optional.of(new GoogleUser(sub, email, name));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private String enc(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  public record GoogleUser(String sub, String email, String name) {}
}
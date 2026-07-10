package com.sun.hades.service;

import com.sun.hades.model.enums.CefrLevel;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Discord OAuth2 code exchange, guild gate, and curated role resolution.
 */
@Service
public class DiscordOAuthService {

  private static final String BASE = "https://discord.com/api";

  /**
   * A learner level granted by a Discord guild role.
   *
   * @param name the display name
   * @param cefrLevel the CEFR equivalent, or null when none applies
   */
  private record Level(String name, CefrLevel cefrLevel) {
  }

  /**
   * The fixed guild role ids that carry a learner level, mapped to their display
   * name and CEFR equivalent.
   */
  private static final Map<String, Level> LEVEL_ROLES = levelRoles();

  private static Map<String, Level> levelRoles() {
    Map<String, Level> roles = new HashMap<>();
    roles.put("352001527780474881", new Level("Non Learner", null));
    roles.put("350483752490631181", new Level("Native", CefrLevel.C2));
    roles.put("351117824300679169", new Level("Beginner", CefrLevel.A1));
    roles.put("351117954974482435", new Level("Elementary", CefrLevel.A2));
    roles.put("350485376109903882", new Level("Intermediate", CefrLevel.B1));
    roles.put("351118486426091521", new Level("Upper Intermediate", CefrLevel.B2));
    roles.put("350485279238258689", new Level("Advanced", CefrLevel.C1));
    roles.put("350483489461895168", new Level("Fluent", CefrLevel.C2));
    return Collections.unmodifiableMap(roles);
  }

  private final String clientId;
  private final String clientSecret;
  private final String guildId;
  private final String redirectUri;
  private final RestClient rest;

  public DiscordOAuthService(
      @Value("${discord.client-id:}") String clientId,
      @Value("${discord.client-secret:}") String clientSecret,
      @Value("${discord.guild-id:}") String guildId,
      @Value("${discord.redirect-uri:}") String redirectUri) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.guildId = guildId;
    this.redirectUri = redirectUri;
    this.rest = RestClient.create();
  }

  /**
   * Exchanges an authorization code for the member's Discord profile.
   *
   * @param code the authorization code returned by Discord
   * @return the verified Discord profile, with roles and CEFR level
   */
  public DiscordProfile exchange(String code) {
    String accessToken = exchangeCode(code);
    Map<String, Object> user = rest.get()
        .uri(BASE + "/users/@me")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .body(Map.class);
    if (user == null) {
      throw new IllegalStateException("Discord returned no user");
    }
    String discordId = String.valueOf(user.get("id"));
    ensureGuildMember(accessToken);
    List<Level> levels = resolveLevels(accessToken);
    List<String> roles = levels.stream()
        .map(Level::name)
        .collect(Collectors.toList());
    CefrLevel level = levels.stream()
        .map(Level::cefrLevel)
        .filter(Objects::nonNull)
        .max(Comparator.comparingInt(Enum::ordinal))
        .orElse(null);
    return new DiscordProfile(
        discordId,
        (String) user.get("username"),
        (String) user.get("global_name"),
        (String) user.get("avatar"),
        level,
        roles);
  }

  private String exchangeCode(String code) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", code);
    form.add("redirect_uri", redirectUri);
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    Map<?, ?> body = rest.post()
        .uri(BASE + "/oauth2/token")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .body(form)
        .retrieve()
        .body(Map.class);
    if (body == null || body.get("access_token") == null) {
      throw new IllegalStateException("Discord token exchange failed");
    }
    return (String) body.get("access_token");
  }

  private void ensureGuildMember(String accessToken) {
    List<?> guilds = rest.get()
        .uri(BASE + "/users/@me/guilds")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .body(List.class);
    if (guilds == null || guilds.stream().noneMatch(g -> guildId.equals(String.valueOf(((Map<?, ?>) g).get("id"))))) {
      throw new IllegalArgumentException("Not a member of the required guild");
    }
  }

  /**
   * Resolves the member's learner levels from their guild role ids.
   *
   * @param accessToken the member's Discord bearer token
   * @return the matched levels, in role order
   */
  private List<Level> resolveLevels(String accessToken) {
    Map<?, ?> member = rest.get()
        .uri(BASE + "/users/@me/guilds/" + guildId + "/member")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .body(Map.class);
    if (member == null) {
      return List.of();
    }
    Object rolesRaw = member.get("roles");
    List<?> roleList = rolesRaw instanceof List<?> list ? list : List.of();
    return roleList.stream()
        .map(String::valueOf)
        .map(LEVEL_ROLES::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * Verified Discord identity for a member.
   */
  public record DiscordProfile(
      String discordId, String username, String globalName, String avatar, CefrLevel cefrLevel,
      List<String> roles) {
  }
}

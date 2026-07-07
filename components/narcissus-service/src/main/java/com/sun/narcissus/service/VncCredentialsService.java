package com.sun.narcissus.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Mints websockify tokens and builds the noVNC iframe src.
 */
@Service
public class VncCredentialsService {

  private static final SecureRandom RANDOM = new SecureRandom();

  @Value("${vnc.token-file:/etc/websockify/tokens}")
  private String tokenFile;

  @Value("${vnc.backend:127.0.0.1:5900}")
  private String backend;

  @Value("${vnc.password:password}")
  private String password;

  @Value("${vnc.websockify-path:/websockify}")
  private String websockifyPath;

  @Value("${vnc.novnc-path:/novnc/vnc.html}")
  private String novncPath;

  /**
   * Builds the noVNC iframe src with a single-use token.
   *
   * @return the iframe src
   */
  public String generateIframeSrc() throws IOException {
    String token = newToken();
    writeToken(token);

    String wsPath = websockifyPath + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    String path = novncPath.startsWith("/") ? novncPath : "/" + novncPath;

    return path
        + "?autoconnect=1&resize=scale&reconnect=1"
        + "&path=" + URLEncoder.encode(wsPath, StandardCharsets.UTF_8)
        + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
  }

  /**
   * Generates a random token.
   *
   * @return the token
   */
  private String newToken() {
    byte[] bytes = new byte[18];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * Writes the token entry to the websockify token file.
   *
   * @param token the token
   */
  private void writeToken(String token) throws IOException {
    Path path = Paths.get(tokenFile);
    String entry = token + "=" + backend + System.lineSeparator();
    Files.writeString(path, entry);
  }
}

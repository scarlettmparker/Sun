package com.sun.dionysus.torrent;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The pieces of a magnet URI worth recording before the download starts.
 *
 * @param infoHash the btih hash string (hex or base32) from the xt parameter
 * @param displayName the decoded dn parameter, or null
 * @param trackers the decoded tr parameters, in order
 */
public record MagnetUri(String infoHash, String displayName, List<String> trackers) {

  /**
   * True when the string looks like a magnet URI.
   */
  public static boolean isMagnet(String value) {
    return value != null && value.startsWith("magnet:?");
  }

  /**
   * Parses a magnet URI into its info hash, display name, and trackers.
   */
  public static MagnetUri parse(String magnet) {
    if (!isMagnet(magnet)) {
      throw new IllegalArgumentException("Not a magnet URI: " + magnet);
    }
    String query = magnet.substring("magnet:?".length());
    String infoHash = null;
    String displayName = null;
    List<String> trackers = new ArrayList<>();
    for (String pair : query.split("&")) {
      int eq = pair.indexOf('=');
      if (eq < 0) {
        continue;
      }
      String key = pair.substring(0, eq);
      String value = decode(pair.substring(eq + 1));
      switch (key) {
        case "xt" -> {
          String[] parts = value.split(":");
          infoHash = parts[parts.length - 1];
        }
        case "dn" -> displayName = value;
        case "tr" -> trackers.add(value);
      }
    }
    if (infoHash == null) {
      throw new IllegalArgumentException("Magnet URI has no info hash: " + magnet);
    }
    return new MagnetUri(infoHash, displayName, List.copyOf(trackers));
  }

  private static String decode(String value) {
    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      return value;
    }
  }
}

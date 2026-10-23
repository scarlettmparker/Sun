package com.sun.dionysus.torrent;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable settings for the embedded torrent client.
 */
@ConfigurationProperties(prefix = "dionysus.torrent")
public class TorrentClientProperties {

  /**
   * Persistent directory holding the per-job scratch folders.
   */
  private String scratchDir =
      System.getProperty("user.home") + "/.local/share/dionysus/torrent";

  /**
   * How many torrents may download concurrently; extras stay queued.
   */
  private int maxConcurrentDownloads = 3;

  public String getScratchDir() {
    return scratchDir;
  }

  public void setScratchDir(String scratchDir) {
    this.scratchDir = scratchDir;
  }

  public int getMaxConcurrentDownloads() {
    return maxConcurrentDownloads;
  }

  public void setMaxConcurrentDownloads(int maxConcurrentDownloads) {
    this.maxConcurrentDownloads = maxConcurrentDownloads;
  }
}

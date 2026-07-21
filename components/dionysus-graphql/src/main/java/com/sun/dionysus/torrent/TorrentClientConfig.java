package com.sun.dionysus.torrent;

import org.libtorrent4j.SessionManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the libtorrent4j session as a singleton bean and binds torrent settings.
 */
@Configuration
@EnableConfigurationProperties(TorrentClientProperties.class)
public class TorrentClientConfig {

  /**
   * The shared libtorrent session. Lifecycle (start/stop) is owned by TorrentClientService.
   */
  @Bean(destroyMethod = "")
  public SessionManager torrentSessionManager() {
    return new SessionManager();
  }
}

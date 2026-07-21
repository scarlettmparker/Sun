package com.sun.dionysus.torrent;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MagnetUri parsing.
 */
class MagnetUriTest {

  @Test
  void parse_extractsHashNameAndTrackers() {
    String magnet =
        "magnet:?xt=urn:btih:abcdef1234567890&dn=Ubuntu%20ISO&tr=http://tracker.example.com&tr=udp://tracker2.example.com:1337";

    MagnetUri parsed = MagnetUri.parse(magnet);

    assertThat(parsed.infoHash()).isEqualTo("abcdef1234567890");
    assertThat(parsed.displayName()).isEqualTo("Ubuntu ISO");
    assertThat(parsed.trackers()).containsExactly(
        "http://tracker.example.com", "udp://tracker2.example.com:1337");
  }

  @Test
  void parse_handlesMissingOptionalFields() {
    MagnetUri parsed = MagnetUri.parse("magnet:?xt=urn:btih:deadbeef");

    assertThat(parsed.infoHash()).isEqualTo("deadbeef");
    assertThat(parsed.displayName()).isNull();
    assertThat(parsed.trackers()).isEmpty();
  }

  @Test
  void parse_rejectsNonMagnet() {
    assertThatThrownBy(() -> MagnetUri.parse("https://example.com/file.torrent"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parse_rejectsMagnetWithoutHash() {
    assertThatThrownBy(() -> MagnetUri.parse("magnet:?dn=NoHash"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void isMagnet_detectsPrefix() {
    assertThat(MagnetUri.isMagnet("magnet:?xt=urn:btih:x")).isTrue();
    assertThat(MagnetUri.isMagnet("not a magnet")).isFalse();
    assertThat(MagnetUri.isMagnet(null)).isFalse();
  }
}

package com.sun.dionysus.subtitle;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SrtToVttTest {

  @Test
  void convertSrt_convertsTimestamps() {
    String srt = "1\n00:00:01,000 --> 00:00:04,000\nHello\n";

    String vtt = SrtToVtt.convert(srt);

    assertThat(vtt).startsWith("WEBVTT");
    assertThat(vtt).contains("00:00:01.000 --> 00:00:04.000");
    assertThat(vtt).contains("Hello");
  }

  @Test
  void convertVtt_passthrough() {
    String vtt = "WEBVTT\n\n00:00:01.000 --> 00:00:04.000\nHello\n";

    assertThat(SrtToVtt.convert(vtt)).isEqualTo(vtt);
  }

  @Test
  void convertBytes_handlesCp1252() throws Exception {
    byte[] data = "1\n00:00:01,000 --> 00:00:04,000\nCafé\n".getBytes(
        java.nio.charset.Charset.forName("windows-1252"));

    String vtt = SrtToVtt.convert(data);

    assertThat(vtt).contains("Café");
  }

  @Test
  void convertBytes_handlesBom() throws Exception {
    byte[] body = "1\n00:00:01,000 --> 00:00:04,000\nHello\n".getBytes(StandardCharsets.UTF_8);
    byte[] data = new byte[body.length + 3];
    data[0] = (byte) 0xef;
    data[1] = (byte) 0xbb;
    data[2] = (byte) 0xbf;
    System.arraycopy(body, 0, data, 3, body.length);

    String vtt = SrtToVtt.convert(data);

    assertThat(vtt).contains("Hello");
    assertThat(vtt).doesNotContain("\uFEFF");
  }

  @Test
  void convert_malformedCueIsSkipped() {
    String srt = "not a cue\n\n1\n00:00:01,000 --> 00:00:04,000\nHello\n";

    String vtt = SrtToVtt.convert(srt);

    assertThat(vtt).contains("Hello");
  }

  @Test
  void convertFile_readsSrt() throws Exception {
    Path file = Files.createTempFile("subtitle-", ".srt");
    Files.writeString(file, "1\n00:00:01,000 --> 00:00:04,000\nHello\n");

    assertThat(SrtToVtt.convert(file)).contains("00:00:01.000 --> 00:00:04.000");
  }
}

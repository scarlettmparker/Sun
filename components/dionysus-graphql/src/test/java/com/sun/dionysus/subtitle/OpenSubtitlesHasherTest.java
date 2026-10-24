package com.sun.dionysus.subtitle;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSubtitlesHasherTest {

  @Test
  void hash_zeroFilledFile() throws Exception {
    byte[] data = new byte[131072];
    Path file = Files.write(Path.of(System.getProperty("java.io.tmpdir"), "hash-zero.bin"), data);

    assertThat(OpenSubtitlesHasher.hash(file)).isEqualTo("0000000000020000");
    assertThat(OpenSubtitlesHasher.compute(file)).isEqualTo("0000000000020000");
  }

  @Test
  void hash_smallFile() throws Exception {
    byte[] data = new byte[8];
    data[0] = 1;
    Path file = Files.write(Path.of(System.getProperty("java.io.tmpdir"), "hash-small.bin"), data);

    assertThat(OpenSubtitlesHasher.hash(file)).isEqualTo("000000000000000a");
  }

  @Test
  void hash_fromComponents() {
    byte[] first = new byte[8];
    first[0] = 1;
    byte[] last = new byte[8];
    last[0] = 2;

    assertThat(OpenSubtitlesHasher.hash(16, first, last)).isEqualTo("0000000000000013");
  }

  @Test
  void hash_emptyFile() throws Exception {
    Path file = Files.write(Path.of(System.getProperty("java.io.tmpdir"), "hash-empty.bin"), new byte[0]);

    assertThat(OpenSubtitlesHasher.hash(file)).isEqualTo("0000000000000000");
  }
}

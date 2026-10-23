package com.sun.dionysus.torrent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TorrentScratch.
 */
class TorrentScratchTest {

  @TempDir Path tempDir;

  @Test
  void hasData_ignoresClientArtefacts() throws IOException {
    Path dir = Files.createDirectory(tempDir.resolve("job"));
    Files.writeString(dir.resolve("source.torrent"), "metadata");
    Files.writeString(dir.resolve(".hidden"), "metadata");
    Files.writeString(dir.resolve("movie.mp4.part"), "partial");

    assertThat(TorrentScratch.hasData(dir.toFile())).isFalse();
  }

  @Test
  void hasData_detectsPayloadFiles() throws IOException {
    Path dir = Files.createDirectory(tempDir.resolve("job"));
    Files.writeString(dir.resolve("movie.mp4"), "payload");

    assertThat(TorrentScratch.hasData(dir.toFile())).isTrue();
  }

  @Test
  void delete_removesDirectoriesInsideRoot() throws IOException {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    Path job = Files.createDirectory(root.resolve("job"));
    Files.writeString(job.resolve("movie.mp4"), "payload");

    TorrentScratch.delete(root.toFile(), job.toFile());

    assertThat(job).doesNotExist();
  }

  @Test
  void delete_skipsDirectoriesOutsideRoot() throws IOException {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    Path other = Files.createDirectory(tempDir.resolve("other"));
    Files.writeString(other.resolve("movie.mp4"), "payload");

    TorrentScratch.delete(root.toFile(), other.toFile());

    assertThat(other).exists();
  }

  @Test
  void delete_skipsTheRootItself() throws IOException {
    Path root = Files.createDirectory(tempDir.resolve("root"));

    TorrentScratch.delete(root.toFile(), root.toFile());

    assertThat(root).exists();
  }
}

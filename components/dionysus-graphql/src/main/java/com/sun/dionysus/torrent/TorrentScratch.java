package com.sun.dionysus.torrent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared helpers for torrent scratch directories.
 */
public final class TorrentScratch {

  private static final Logger log = LoggerFactory.getLogger(TorrentScratch.class);

  private TorrentScratch() {}

  /**
   * Deletes a directory only when it sits inside the scratch root.
   *
   * @param root the configured scratch root.
   * @param target the directory to remove.
   */
  public static void delete(File root, File target) {
    if (root == null || target == null) {
      return;
    }
    try {
      Path rootPath = root.getCanonicalFile().toPath();
      Path targetPath = target.getCanonicalFile().toPath();
      if (targetPath.equals(rootPath) || !targetPath.startsWith(rootPath)) {
        return;
      }
      deleteRecursively(targetPath);
    } catch (IOException e) {
      log.warn("Could not resolve scratch path {}: {}", target, e.getMessage());
    }
  }

  /**
   * Deletes a directory tree without following symbolic links.
   *
   * @param path the directory to remove.
   */
  public static void deleteRecursively(Path path) {
    if (path == null || !Files.exists(path)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(path)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(
              entry -> {
                try {
                  Files.deleteIfExists(entry);
                } catch (IOException ignored) {
                }
              });
    } catch (IOException e) {
      log.warn("Could not delete scratch path {}: {}", path, e.getMessage());
    }
  }

  /**
   * Reports whether a directory holds downloaded data.
   *
   * @param dir the job download directory.
   * @return true when at least one data file exists.
   */
  public static boolean hasData(File dir) {
    if (dir == null || !dir.isDirectory()) {
      return false;
    }
    try (Stream<Path> paths = Files.walk(dir.toPath())) {
      return paths.filter(Files::isRegularFile).anyMatch(TorrentScratch::isDataFile);
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Reports whether a file belongs to the downloaded payload.
   *
   * @param file the candidate file.
   * @return true when the file is not a client artefact.
   */
  private static boolean isDataFile(Path file) {
    String name = file.getFileName().toString();
    return !name.endsWith(".torrent")
        && !name.startsWith(".")
        && !name.endsWith(".part")
        && !name.endsWith(".parts")
        && !name.endsWith(".!qB")
        && !name.endsWith(".!ut");
  }
}

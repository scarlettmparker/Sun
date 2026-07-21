package com.sun.dionysus.torrent;

import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.File;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Removes scratch directories left behind by deleted or lost jobs, so restarts
 * do not leak disk.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TorrentScratchSweeper implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(TorrentScratchSweeper.class);

  @Autowired private TorrentJobService jobService;
  @Autowired private TorrentClientProperties properties;

  @Override
  public void run(ApplicationArguments args) {
    File root = new File(properties.getScratchDir());
    if (!root.isDirectory()) {
      return;
    }
    Set<String> known =
        jobService.findAll().stream()
            .map(job -> new File(job.getScratchPath()).getAbsolutePath())
            .collect(Collectors.toSet());

    File[] children = root.listFiles();
    if (children == null) {
      return;
    }
    for (File child : children) {
      if (child.isDirectory() && !known.contains(child.getAbsolutePath())) {
        deleteRecursively(child);
        logger.info("Swept orphan scratch dir {}", child.getAbsolutePath());
      }
    }
  }

  private void deleteRecursively(File file) {
    try (var paths = Files.walk(file.toPath())) {
      paths.sorted(java.util.Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
              });
    } catch (Exception ignored) {
    }
  }
}

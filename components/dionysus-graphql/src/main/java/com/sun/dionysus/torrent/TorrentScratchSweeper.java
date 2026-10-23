package com.sun.dionysus.torrent;

import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Removes scratch directories left behind by finished or lost jobs.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TorrentScratchSweeper implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(TorrentScratchSweeper.class);

  @Autowired private TorrentJobService jobService;
  @Autowired private TorrentClientProperties properties;

  /**
   * Deletes scratch directories that no unfinished job owns.
   *
   * @param args the application arguments.
   */
  @Override
  public void run(ApplicationArguments args) {
    File root = new File(properties.getScratchDir());
    if (!root.isDirectory()) {
      return;
    }

    Set<String> known = knownPaths();
    File[] children = root.listFiles();
    if (children == null) {
      return;
    }

    for (File child : children) {
      if (child.isDirectory() && !known.contains(child.getAbsolutePath())) {
        TorrentScratch.deleteRecursively(child.toPath());
        logger.info("Swept orphan scratch dir {}", child.getAbsolutePath());
      }
    }
  }

  /**
   * Collects the scratch paths owned by unfinished jobs.
   *
   * @return the absolute paths to keep.
   */
  private Set<String> knownPaths() {
    return jobService.findAll().stream()
        .filter(job -> TorrentJobService.VISIBLE_STATUSES.contains(job.getStatus()))
        .map(job -> job.getScratchPath())
        .filter(path -> path != null && !path.isBlank())
        .map(path -> new File(path).getAbsolutePath())
        .collect(Collectors.toSet());
  }
}

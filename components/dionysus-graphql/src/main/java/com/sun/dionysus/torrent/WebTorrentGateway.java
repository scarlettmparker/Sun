package com.sun.dionysus.torrent;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Downloads torrents via the webtorrent-cli Node.js process — no native library
 * required. Spawns {@code webtorrent download <magnet|torrent> --json --out <dir>}
 * and parses the JSON progress lines to update the job in the database.
 */
@Component
public class WebTorrentGateway {

  private static final Logger log = LoggerFactory.getLogger(WebTorrentGateway.class);

  private static final String WEBTORRENT_CMD;

  static {
    String cmd = resolveWebtorrent();
    WEBTORRENT_CMD = cmd;
    log.info("WebTorrent binary resolved to: {}", cmd);
  }

  /**
   * Searches for the webtorrent binary in node_modules/.bin relative to common
   * working directories, falling back to "npx webtorrent".
   */
  private static String resolveWebtorrent() {
    String[] candidates = {
      "node_modules/.bin/webtorrent",
      "../node_modules/.bin/webtorrent",
      "../../node_modules/.bin/webtorrent",
    };
    for (String c : candidates) {
      Path p = Paths.get(c);
      if (Files.isExecutable(p)) return p.toAbsolutePath().toString();
    }
    // Fallback: rely on npx finding it
    return "npx";
  }

  @Autowired private TorrentJobService jobService;

  /**
   * Downloads a magnet link via webtorrent-cli.
   */
  @Async("torrentTaskExecutor")
  public void downloadMagnet(UUID jobId, String magnet, File saveDir) {
    download(jobId, magnet, saveDir);
  }

  /**
   * Downloads a .torrent file (written to a temp file) via webtorrent-cli.
   */
  @Async("torrentTaskExecutor")
  public void downloadTorrentFile(UUID jobId, File torrentFile, File saveDir) {
    download(jobId, torrentFile.getAbsolutePath(), saveDir);
  }

  private void download(UUID jobId, String source, File saveDir) {
    saveDir.mkdirs();

    String cmd;
    if ("npx".equals(WEBTORRENT_CMD)) {
      cmd = "npx webtorrent";
      log.info("Starting webtorrent via npx for job {} in dir {}", jobId, saveDir);
    } else {
      cmd = WEBTORRENT_CMD;
      log.info("Starting webtorrent at {} for job {} in dir {}", cmd, jobId, saveDir);
    }

    ProcessBuilder pb;
    if ("npx".equals(WEBTORRENT_CMD)) {
      pb = new ProcessBuilder("npx", "webtorrent", "download", source,
          "--out", saveDir.getAbsolutePath(), "--json");
    } else {
      pb = new ProcessBuilder(WEBTORRENT_CMD, "download", source,
          "--out", saveDir.getAbsolutePath(), "--json");
    }
    pb.environment().put("PATH", System.getenv("PATH"));
    pb.directory(new File("."));
    pb.redirectErrorStream(true);

    try {
      Process process = pb.start();
      log.info("Webtorrent process started for job {} (pid?)", jobId);

      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (line.startsWith("{")) {
            parseAndUpdate(jobId, line);
          } else if (!line.isEmpty()) {
            log.debug("Webtorrent output for job {}: {}", jobId, line);
          }
        }
      }

      int exitCode = process.waitFor();
      log.info("Webtorrent process exited with code {} for job {}", exitCode, jobId);
      updateDone(jobId, exitCode == 0);

    } catch (Exception e) {
      log.error("Webtorrent process failed for job {}", jobId, e);
      updateFailed(jobId, e.getMessage());
    }
  }

  /**
   * Parses a JSON progress line from webtorrent and updates the job.
   */
  private void parseAndUpdate(UUID jobId, String jsonLine) {
    if (jsonLine == null || jsonLine.isEmpty() || !jsonLine.startsWith("{")) return;
    try {
      com.fasterxml.jackson.databind.JsonNode node =
          new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonLine);

      double progress = node.has("progress") ? node.get("progress").asDouble(0.0) : 0.0;
      long downloaded = node.has("downloaded") ? node.get("downloaded").asLong(0) : 0;
      long total = node.has("length") ? node.get("length").asLong(0) : 0;
      int speedBps = node.has("downloadSpeed") ? node.get("downloadSpeed").asInt(0) : 0;
      int peers = node.has("numPeers") ? node.get("numPeers").asInt(0) : 0;
      long eta = node.has("timeRemaining") ? node.get("timeRemaining").asLong(0) : 0;

      TorrentStatus status = progress >= 1.0 ? TorrentStatus.COMPLETED : TorrentStatus.DOWNLOADING;

      TorrentJobEntity job = jobService.findById(jobId).orElse(null);
      if (job == null) return;

      job.setStatus(status);
      job.setProgress(progress);
      job.setDownloadedBytes(downloaded);
      job.setTotalBytes(total);
      job.setDownloadRateBps(speedBps);
      job.setPeersConnected(peers);
      job.setEtaSeconds(eta);
      jobService.save(job);
    } catch (Exception e) {
      log.warn("Failed to parse webtorrent JSON for job {}: {}", jobId, e.getMessage());
    }
  }

  private void updateDone(UUID jobId, boolean success) {
    jobService.findById(jobId).ifPresent(job -> {
      if (success) {
        job.setStatus(TorrentStatus.COMPLETED);
        job.setProgress(1.0);
        job.setDownloadRateBps(0);
        job.setPeersConnected(0);
        job.setEtaSeconds(0L);
      } else {
        job.setStatus(TorrentStatus.FAILED);
        job.setErrorMessage("webtorrent process exited with error");
      }
      jobService.save(job);
    });
  }

  private void updateFailed(UUID jobId, String error) {
    jobService.findById(jobId).ifPresent(job -> {
      job.setStatus(TorrentStatus.FAILED);
      job.setErrorMessage(error);
      jobService.save(job);
    });
  }
}

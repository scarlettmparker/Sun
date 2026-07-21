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
 * Downloads torrents via the webtorrent-cli Node.js process.
 * Spawns {@code webtorrent download <magnet|torrent> --json --out <dir>}
 * and parses the JSON progress lines to update the job in the database.
 */
@Component
public class WebTorrentGateway {

  private static final Logger log = LoggerFactory.getLogger(WebTorrentGateway.class);

  private static final String WRAPPER_SCRIPT = """
      import { createHash } from 'crypto'
      const WebTorrent = (await import('webtorrent')).default
      const src = process.argv[2], dest = process.argv[3]
      if (!src || !dest) { process.exit(1) }
      const key = createHash('sha1').update(src).digest('hex').slice(0, 20)
      const client = new WebTorrent({ maxConns: 200, uploads: 5, tracker: true, dht: true, utp: true, tcp: true })
      let running = true
      function die(c) { if (!running) return; running = false; client.destroy(() => process.exit(c)) }
      const tor = client.add(src, { path: dest, name: key })
      function r() { console.log(JSON.stringify({progress: tor.progress, downloaded: tor.downloaded, length: tor.length || 1, downloadSpeed: tor.downloadSpeed, numPeers: tor.numPeers, timeRemaining: tor.timeRemaining})) }
      tor.on('download', r)
      tor.on('done', () => { r(); die(0) })
      tor.on('error', (e) => { console.error('err:', e.message); die(1) })
      tor.on('warning', (e) => console.error('warn:', e.message))
      setInterval(() => { if (tor.progress >= 1) die(0); r() }, 10000)
      """;

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

    Path scriptDir = Paths.get(".").toAbsolutePath().normalize();
    if (!Files.isDirectory(scriptDir.resolve("node_modules"))) {
      scriptDir = scriptDir.getParent();
    }
    File scriptFile = new File(scriptDir.toFile(), "webtorrent-dl.mjs");
    try { Files.write(scriptFile.toPath(), WRAPPER_SCRIPT.getBytes(StandardCharsets.UTF_8)); } catch (Exception e) {
      updateFailed(jobId, "Failed to write wrapper script");
      return;
    }
    String node = Paths.get(System.getProperty("user.home"), ".config/nvm/versions/node/v20.20.2/bin/node").toString();
    if (!Files.isExecutable(Paths.get(node))) node = "node";
    String[] fullCmd = new String[]{node, scriptFile.getAbsolutePath(), source, saveDir.getAbsolutePath()};
    log.info("Starting webtorrent: {} {}", node, scriptFile.getAbsolutePath());

    ProcessBuilder pb = new ProcessBuilder(fullCmd);
    pb.environment().put("PATH", System.getenv("PATH"));
    pb.redirectErrorStream(true);

    // Retry loop: if the process exits non-zero, restart it forever until cancelled
    StringBuilder output = new StringBuilder();

    for (int attempt = 1; ; attempt++) {
      // Stop retrying if the job was cancelled
      if (jobService.findById(jobId).map(j -> j.getStatus() == TorrentStatus.CANCELLED).orElse(false)) {
        log.info("Job {} was cancelled, stopping retry", jobId);
        return;
      }
      try {
        Process process = pb.start();
        log.info("Webtorrent process started for job {} (attempt {})", jobId, attempt);

        StringBuilder jsonBuf = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

          int ch;
          while ((ch = reader.read()) != -1) {
            char c = (char) ch;
            jsonBuf.append(c);
            if (c == '\n') {
              String line = jsonBuf.toString().trim();
              jsonBuf.setLength(0);
              if (line.startsWith("{")) {
                parseAndUpdate(jobId, line);
              } else if (!line.isEmpty()) {
                log.info("Webtorrent output for job {}: {}", jobId, line);
                if (output.length() < 2000) output.append(line).append(" ");
              }
            }
          }
        }

        int exitCode = process.waitFor();
        log.info("Webtorrent process exited with code {} for job {}", exitCode, jobId);

        if (exitCode == 0) {
          updateDone(jobId, true, "");
          return;
        }

        // Reset attempt counter if any progress was made
        long downloaded = jobService.findById(jobId).map(TorrentJobEntity::getDownloadedBytes).orElse(0L);
        if (downloaded > 0) {
          log.info("Progress made ({} bytes), resetting attempt counter", downloaded);
          attempt = 0;
        }
        log.warn("Webtorrent attempt {} failed for job {}, retrying...", attempt, jobId);
        // Don't overwrite CANCELLED, user may have cancelled during the attempt
        if (jobService.findById(jobId).map(j -> j.getStatus() == TorrentStatus.CANCELLED).orElse(false)) {
          log.info("Job {} was cancelled, stopping retry", jobId);
          return;
        }
        int attemptFinal = attempt;
        jobService.findById(jobId).ifPresent(job -> {
          job.setStatus(TorrentStatus.DOWNLOADING);
          job.setErrorMessage("Retrying after attempt " + attemptFinal);
          jobService.save(job);
        });
        Thread.sleep(2000L * Math.min(attempt + 1, 30));

      } catch (Exception e) {
        log.error("Webtorrent attempt {} failed for job {}", attempt, jobId, e);
        try { Thread.sleep(2000L * Math.min(attempt, 30)); } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
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
      job.setTotalBytes(total != 0 ? total : job.getTotalBytes());
      job.setDownloadRateBps(speedBps);
      job.setPeersConnected(peers);
      job.setEtaSeconds(eta);
      jobService.save(job);
    } catch (Exception e) {
      log.warn("Failed to parse webtorrent JSON for job {}: {}", jobId, e.getMessage());
    }
  }

  private void updateDone(UUID jobId, boolean success, String output) {
    jobService.findById(jobId).ifPresent(job -> {
      if (success) {
        job.setStatus(TorrentStatus.COMPLETED);
        job.setProgress(1.0);
        job.setDownloadRateBps(0);
        job.setPeersConnected(0);
        job.setEtaSeconds(0L);
      } else {
        job.setStatus(TorrentStatus.FAILED);
        job.setErrorMessage(output.isEmpty() ? "webtorrent process exited with error" : output);
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

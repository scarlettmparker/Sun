package com.sun.dionysus.torrent;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Downloads torrents via aria2c. Spawns {@code aria2c <magnet> --dir=<dir>}
 * and parses progress output. Supports resume via .aria2 control files.
 */
@Component
public class Aria2Gateway {

  private static final Logger log = LoggerFactory.getLogger(Aria2Gateway.class);

  private static final Pattern PROGRESS_PATTERN =
      Pattern.compile("\\((\\d+(\\.\\d+)?)%\\)");

  @Autowired private TorrentJobService jobService;

  @Async("torrentTaskExecutor")
  public void downloadMagnet(UUID jobId, String magnet, File saveDir) {
    download(jobId, magnet, saveDir);
  }

  @Async("torrentTaskExecutor")
  public void downloadTorrentFile(UUID jobId, File torrentFile, File saveDir) {
    download(jobId, torrentFile.getAbsolutePath(), saveDir);
  }

  private void download(UUID jobId, String source, File saveDir) {
    saveDir.mkdirs();

    for (int attempt = 1; ; attempt++) {
      if (jobService.findById(jobId).map(j -> j.getStatus() == TorrentStatus.CANCELLED).orElse(false)) {
        log.info("Job {} was cancelled", jobId);
        return;
      }

      ProcessBuilder pb = new ProcessBuilder(
          "aria2c", source,
          "--dir=" + saveDir.getAbsolutePath(),
          "--max-connection-per-server=16",
          "--split=16",
          "--bt-max-peers=100",
          "--peer-id-prefix=-UT2210-",
          "--enable-dht=true",
          "--dht-listen-port=6881",
          "--enable-dht6=true",
          "--dht-file-path=" + saveDir.getAbsolutePath() + "/dht.dat",
          "--follow-torrent=false",
          "--summary-interval=5",
          "--console-log-level=notice",
          "--allow-overwrite=true",
          "--auto-file-renaming=false",
          "--continue=true"
      );
      pb.redirectErrorStream(true);

      try {
        Process process = pb.start();
        log.info("aria2c started for job {} (attempt {})", jobId, attempt);
        long lastReport = 0;

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
          String line;
          while ((line = reader.readLine()) != null) {
            if (line.contains("DL:")) {
              long now = System.currentTimeMillis();
              if (now - lastReport > 2000) {
                lastReport = now;
                long downloaded = parseBytes(line, "DL:");
                long total = parseBytes(line, "/");
                double progress = parseProgress(line);
                int speed = parseSpeed(line);

                TorrentJobEntity job = jobService.findById(jobId).orElse(null);
                if (job != null) {
                  job.setStatus(TorrentStatus.DOWNLOADING);
                  job.setProgress(progress);
                  job.setDownloadedBytes(downloaded);
                  job.setTotalBytes(total);
                  job.setDownloadRateBps(speed);
                  job.setPeersConnected(parsePeers(line));
                  jobService.save(job);
                }
                log.info("aria2c progress for {}: {}% {}B/s", jobId, String.format("%.1f", progress * 100), speed);
              }
            }
          }
        }

        int exitCode = process.waitFor();
        log.info("aria2c exited with code {} for job {}", exitCode, jobId);

        if (exitCode == 0) {
          markDone(jobId);
          return;
        }

        Thread.sleep(3000L);

      } catch (Exception e) {
        log.error("aria2c failed for job {}", jobId, e);
        try { Thread.sleep(3000L); } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  private void markDone(UUID jobId) {
    jobService.findById(jobId).ifPresent(job -> {
      job.setStatus(TorrentStatus.COMPLETED);
      job.setProgress(1.0);
      job.setDownloadRateBps(0);
      jobService.save(job);
    });
  }

  private double parseProgress(String line) {
    Matcher m = PROGRESS_PATTERN.matcher(line);
    return m.find() ? Double.parseDouble(m.group(1)) / 100.0 : 0.0;
  }

  private long parseBytes(String line, String prefix) {
    try {
      int idx = line.indexOf(prefix);
      if (idx < 0) return 0;
      String rest = line.substring(idx + prefix.length()).trim().split("\\s")[0];
      return parseSize(rest);
    } catch (Exception e) {
      return 0;
    }
  }

  private int parseSpeed(String line) {
    try {
      int idx = line.indexOf("DL:");
      if (idx < 0) return 0;
      // Speed is after DL: value like "2.5MiB"
      String after = line.substring(idx + 3).trim();
      String[] parts = after.split("\\s");
      if (parts.length < 2) return 0;
      return (int) parseSize(parts[1]);
    } catch (Exception e) {
      return 0;
    }
  }

  private int parsePeers(String line) {
    try {
      int idx = line.indexOf("CN:");
      if (idx < 0) return 0;
      String after = line.substring(idx + 3).trim();
      return Integer.parseInt(after.split("\\s")[0]);
    } catch (Exception e) {
      return 0;
    }
  }

  private long parseSize(String s) {
    if (s == null || s.isEmpty()) return 0;
    s = s.trim();
    double val = Double.parseDouble(s.replaceAll("[^0-9.]", ""));
    if (s.endsWith("KiB")) return (long) (val * 1024);
    if (s.endsWith("MiB")) return (long) (val * 1024 * 1024);
    if (s.endsWith("GiB")) return (long) (val * 1024 * 1024 * 1024);
    if (s.endsWith("B")) return (long) val;
    return (long) val;
  }
}

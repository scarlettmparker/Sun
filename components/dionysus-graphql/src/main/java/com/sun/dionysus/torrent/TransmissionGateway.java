package com.sun.dionysus.torrent;

import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Downloads torrents via transmission-daemon. Spawns transmission-remote to add
 * magnets and polls for progress. Transmission handles piece-level resume natively.
 */
@Component
public class TransmissionGateway {

  private static final Logger log = LoggerFactory.getLogger(TransmissionGateway.class);

  private static final String[] BASE = {"transmission-remote", "-n", "transmission:transmission"};

  @Autowired private TorrentJobService jobService;
  @Autowired private TorrentCompletionService completionService;

  /**
   * Starts a magnet download via transmission-daemon.
   */
  @Async("torrentTaskExecutor")
  public void downloadMagnet(UUID jobId, String magnet, File saveDir) {
    download(jobId, magnet, saveDir);
  }

  /**
   * Starts a .torrent file download via transmission-daemon.
   */
  @Async("torrentTaskExecutor")
  public void downloadTorrentFile(UUID jobId, File torrentFile, File saveDir) {
    download(jobId, torrentFile.getAbsolutePath(), saveDir);
  }

  /**
   * Adds the torrent to transmission and polls for progress until completion or cancel.
   *
   * @param source magnet URI or torrent file path.
   */
  private void download(UUID jobId, String source, File saveDir) {
    log.info("Starting download for job {} in {}", jobId, saveDir);
    saveDir.mkdirs();
    exec("--add", source);

    String infoHash = jobService.findById(jobId).map(TorrentJobEntity::getInfoHash).orElse(null);
    String tid = null;
    for (int i = 0; i < 10 && tid == null; i++) {
      try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
      tid = findTorrentId(exec("-l"), infoHash);
    }
    if (tid == null) {
      log.warn("Torrent never appeared in transmission for job {}", jobId);
      return;
    }
    final String torrentId = tid;

    while (true) {
      if (isCancelled(jobId)) { removeTransmission(jobId); return; }
      try { Thread.sleep(4000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }

      String status = parseValueStr(exec("-t", torrentId, "-i"), "  Status:", "(.*)");
      if (status != null && !status.toLowerCase().contains("downloading") && !status.toLowerCase().contains("verify")) {
        exec("-t", torrentId, "--start");
        try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
      }

      String details = exec("-t", torrentId, "-i");
      if (details == null) continue;

      double progress = parseValue(details, "Percent Done:", "(\\d+\\.?\\d*)") / 100.0;
      long downloaded = parseBytes(details, "Have:");
      long total = parseBytes(details, "Total size:");
      int rate = (int) parseBytes(details, "Download Speed:");
      int peers = (int) parseValue(details, "Peers:", "(\\d+)");

      if (isCancelled(jobId)) { removeTransmission(jobId); return; }

      TorrentJobEntity job = jobService.findById(jobId).orElse(null);
      if (job == null) { removeTransmission(jobId); return; }

      job.setStatus(TorrentStatus.DOWNLOADING);
      job.setProgress(progress);
      job.setDownloadedBytes(downloaded);
      job.setTotalBytes(total);
      job.setDownloadRateBps(rate);
      job.setPeersConnected(peers);
      jobService.save(job);
      log.info("transmission progress for {}: {}% {}", jobId, String.format("%.1f", progress * 100), formatRate(rate));

      if (progress >= 1.0) {
        log.info("Download complete for job {}...", jobId);
        TorrentJobEntity j = jobService.findById(jobId).orElse(null);
        if (j != null && j.getStatus() != TorrentStatus.COMPLETED) {
          completionService.complete(jobId);
        }
        return;
      }
    }
  }

  /**
   * Moves all files from one directory to another, preserving subdirectory structure.
   */
  private void moveFiles(File from, File to) {
    if (!from.isDirectory()) return;
    log.info("Moving files from {} to {}", from, to);
    try (var paths = java.nio.file.Files.walk(from.toPath())) {
      paths.filter(java.nio.file.Files::isRegularFile).forEach(f -> {
        try {
          Path rel = from.toPath().relativize(f);
          Path dest = new java.io.File(to, rel.toString()).toPath();
          dest.getParent().toFile().mkdirs();
          java.nio.file.Files.move(f, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
          log.warn("Failed to move {}: {}", f.getFileName(), e.getMessage());
        }
      });
    } catch (Exception e) {
      log.warn("Failed to walk {}", from, e);
    }
  }

  /**
   * Removes the torrent from transmission by matching the info hash.
   */
  private void removeTransmission(UUID jobId) {
    String infoHash = jobService.findById(jobId).map(TorrentJobEntity::getInfoHash).orElse(null);
    if (infoHash == null) return;
    String list = exec("-l");
    if (list == null) return;
    for (String line : list.split("\n")) {
      String id = new String(line).replaceAll("^\\s*(\\d+).*", "$1").trim();
      if (id.matches("\\d+")) {
        String details = exec("-t", id, "-i");
        if (details != null && details.toLowerCase().contains(infoHash.toLowerCase())) {
          exec("-t", id, "--remove-and-delete");
          return;
        }
      }
    }
  }

  /**
   * Whether the job was cancelled by the user.
   */
  private boolean isCancelled(UUID jobId) {
    return jobService.findById(jobId).map(j -> j.getStatus() == TorrentStatus.CANCELLED).orElse(false);
  }

  /**
   * Runs transmission-remote with the given arguments and returns stdout.
   *
   * @return stdout, or null on failure.
   */
  private String exec(String... args) {
    try {
      String[] cmd = new String[BASE.length + args.length];
      System.arraycopy(BASE, 0, cmd, 0, BASE.length);
      System.arraycopy(args, 0, cmd, BASE.length, args.length);
      Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
      String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      p.waitFor();
      return out;
    } catch (Exception e) {
      log.warn("transmission-remote exec failed: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Finds the torrent ID by matching the info hash in its details.
   */
  private String findTorrentId(String list, String infoHash) {
    if (infoHash == null || infoHash.isEmpty()) {
      for (String line : list.split("\n")) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("Sum:") || line.startsWith("ID")) continue;
        String[] parts = line.split("\\s+");
        if (parts.length < 2) continue;
        String id = parts[0].replaceAll("[^0-9]", "");
        if (!id.isEmpty()) return id;
      }
      return null;
    }
    for (String line : list.split("\n")) {
      line = line.trim();
      if (line.isEmpty() || line.startsWith("Sum:") || line.startsWith("ID")) continue;
      String[] parts = line.split("\\s+");
      if (parts.length < 2) continue;
      String id = parts[0].replaceAll("[^0-9]", "");
      if (!id.isEmpty()) {
        String details = exec("-t", id, "-i");
        if (details != null && details.toLowerCase().contains(infoHash.toLowerCase())) return id;
      }
    }
    return null;
  }

  /**
   * Parses a numeric value from transmission-remote -i output.
   *
   * @param output the full -i output.
   * @param label  the line label to match (e.g. "Percent Done:").
   * @param regex  a capturing regex for the value on the matched line.
   */
  private double parseValue(String output, String label, String regex) {
    for (String line : output.split("\n")) {
      if (line.contains(label)) {
        Matcher m = Pattern.compile(regex).matcher(line);
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
      }
    }
    return 0;
  }

  /**
   * Parses a string value from transmission-remote -i output.
   *
   * @param output the full -i output.
   * @param label  the line label to match (e.g. "  Status:").
   * @param regex  a capturing regex for the value on the matched line.
   */
  private String parseValueStr(String output, String label, String regex) {
    if (output == null) return null;
    for (String line : output.split("\n")) {
      if (line.contains(label)) {
        Matcher m = Pattern.compile(regex).matcher(line);
        return m.find() ? m.group(1).trim() : null;
      }
    }
    return null;
  }

  /**
   * Parses a byte count from a labelled line in transmission output.
   */
  private long parseBytes(String output, String label) {
    for (String line : output.split("\n")) {
      if (line.contains(label)) {
        line = line.replaceAll(".*" + label + "\\s*", "").trim();
        return parseSize(line);
      }
    }
    return 0;
  }

  /**
   * Formats a byte rate for human-readable logging.
   */
  private String formatRate(long bytesPerSec) {
    if (bytesPerSec >= 1_000_000_000) return String.format("%.1f GB/s", bytesPerSec / 1_000_000_000.0);
    if (bytesPerSec >= 1_000_000) return String.format("%.1f MB/s", bytesPerSec / 1_000_000.0);
    if (bytesPerSec >= 1_000) return String.format("%.1f KB/s", bytesPerSec / 1_000.0);
    return bytesPerSec + " B/s";
  }

  /**
   * Converts a size string (e.g. "3.2 MB") to bytes.
   */
  private long parseSize(String s) {
    try {
      double val = Double.parseDouble(s.replaceAll("[^0-9.]", ""));
      if (s.contains("TiB")) return (long) (val * 1024L * 1024 * 1024 * 1024);
      if (s.contains("GiB") || s.contains("GB/s")) return (long) (val * 1024L * 1024 * 1024);
      if (s.contains("GB")) return (long) (val * 1000L * 1000 * 1000);
      if (s.contains("MiB") || s.contains("MB/s")) return (long) (val * 1024L * 1024);
      if (s.contains("MB")) return (long) (val * 1000L * 1000);
      if (s.contains("KiB") || s.contains("KB/s")) return (long) (val * 1024);
      if (s.contains("KB")) return (long) (val * 1000);
      if (s.contains("B")) return (long) val;
      return (long) val;
    } catch (Exception e) { return 0; }
  }
}

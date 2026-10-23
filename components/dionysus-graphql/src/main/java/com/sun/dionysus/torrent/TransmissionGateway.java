package com.sun.dionysus.torrent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Downloads torrents through transmission-daemon using its JSON-RPC API.
 */
@Component
public class TransmissionGateway {

  private static final Logger log = LoggerFactory.getLogger(TransmissionGateway.class);

  private static final String[] TORRENT_FIELDS = {
    "id",
    "hashString",
    "name",
    "percentDone",
    "downloadedEver",
    "sizeWhenDone",
    "totalSize",
    "rateDownload",
    "peersConnected",
    "status",
    "error",
    "errorString",
    "downloadDir"
  };

  private static final int STATUS_STOPPED = 0;
  private static final int ERROR_LOCAL = 3;

  private final ObjectMapper mapper = new ObjectMapper();

  @Autowired private TransmissionRpcClient rpc;
  @Autowired private TorrentJobService jobService;
  @Autowired private TorrentCompletionService completionService;

  /**
   * Starts a new magnet download.
   *
   * @param jobId the torrent job id.
   * @param magnet the magnet URI.
   * @param saveDir the job download directory.
   */
  @Async("torrentTaskExecutor")
  public void downloadMagnet(UUID jobId, String magnet, File saveDir) {
    download(jobId, magnet, saveDir, false);
  }

  /**
   * Resumes a magnet download that already owns local data.
   *
   * @param jobId the torrent job id.
   * @param magnet the magnet URI.
   * @param saveDir the job download directory.
   */
  @Async("torrentTaskExecutor")
  public void resumeMagnet(UUID jobId, String magnet, File saveDir) {
    download(jobId, magnet, saveDir, true);
  }

  /**
   * Starts a new .torrent download.
   *
   * @param jobId the torrent job id.
   * @param torrentFile the .torrent file on disk.
   * @param saveDir the job download directory.
   */
  @Async("torrentTaskExecutor")
  public void downloadTorrentFile(UUID jobId, File torrentFile, File saveDir) {
    download(jobId, torrentFile.getAbsolutePath(), saveDir, false);
  }

  /**
   * Resumes a .torrent download that already owns local data.
   *
   * @param jobId the torrent job id.
   * @param torrentFile the .torrent file on disk.
   * @param saveDir the job download directory.
   */
  @Async("torrentTaskExecutor")
  public void resumeTorrentFile(UUID jobId, File torrentFile, File saveDir) {
    download(jobId, torrentFile.getAbsolutePath(), saveDir, true);
  }

  /**
   * Removes the torrent owned by a job from transmission.
   *
   * @param jobId the torrent job id.
   */
  public void removeTransmission(UUID jobId) {
    removeTorrentByInfoHash(infoHashFor(jobId));
  }

  /**
   * Removes the torrent owned by a job, optionally deleting its files.
   *
   * @param jobId the torrent job id.
   * @param deleteLocalData whether transmission should delete the downloaded files.
   */
  public void removeTransmission(UUID jobId, boolean deleteLocalData) {
    removeTorrentByInfoHash(infoHashFor(jobId), deleteLocalData);
  }

  /**
   * Removes every transmission torrent with the given info hash.
   *
   * @param infoHash the torrent info hash.
   */
  public void removeTorrentByInfoHash(String infoHash) {
    removeTorrentByInfoHash(infoHash, false);
  }

  /**
   * Removes every transmission torrent with the given info hash.
   *
   * @param infoHash the torrent info hash.
   * @param deleteLocalData whether transmission should delete the downloaded files.
   */
  public void removeTorrentByInfoHash(String infoHash, boolean deleteLocalData) {
    if (infoHash == null || infoHash.isBlank()) {
      return;
    }
    removeTorrentsByInfoHash(List.of(infoHash), deleteLocalData);
  }

  /**
   * Removes every transmission torrent matching the given info hashes.
   *
   * @param infoHashes the torrent info hashes.
   */
  public void removeTorrentsByInfoHash(Collection<String> infoHashes) {
    removeTorrentsByInfoHash(infoHashes, false);
  }

  /**
   * Removes every transmission torrent matching the given info hashes.
   *
   * @param infoHashes the torrent info hashes.
   * @param deleteLocalData whether transmission should delete the downloaded files.
   */
  public void removeTorrentsByInfoHash(Collection<String> infoHashes, boolean deleteLocalData) {
    Set<String> normalized =
        infoHashes.stream()
            .filter(hash -> hash != null && !hash.isBlank())
            .map(hash -> hash.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    if (normalized.isEmpty()) {
      return;
    }

    JsonNode torrents = getTorrents();
    if (torrents == null) {
      return;
    }

    ArrayNode ids = mapper.createArrayNode();
    for (JsonNode torrent : torrents) {
      String hash = torrent.path("hashString").asText("").toLowerCase(Locale.ROOT);
      if (normalized.contains(hash)) {
        ids.add(torrent.path("id").asInt());
      }
    }
    removeTorrentIds(ids, deleteLocalData);
  }

  /**
   * Removes transmission torrents by id.
   *
   * @param ids the transmission torrent ids.
   * @param deleteLocalData whether transmission should delete the downloaded files.
   */
  private void removeTorrentIds(ArrayNode ids, boolean deleteLocalData) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    ObjectNode arguments = mapper.createObjectNode();
    arguments.set("ids", ids);
    arguments.put("delete-local-data", deleteLocalData);
    rpc.call("torrent-remove", arguments);
    log.info("Removed transmission torrents {}", ids);
  }

  /**
   * Runs a download until it finishes, fails, or is cancelled.
   *
   * @param jobId the torrent job id.
   * @param source the magnet URI or .torrent file path.
   * @param saveDir the job download directory.
   * @param resume whether an existing torrent should be reused.
   */
  private void download(UUID jobId, String source, File saveDir, boolean resume) {
    log.info("Starting download for job {} in {}", jobId, saveDir);
    ensureDir(saveDir);

    String infoHash = infoHashFor(jobId);
    if (infoHash == null) {
      fail(jobId, "Torrent job has no info hash");
      return;
    }

    boolean resumeWithData = resume && TorrentScratch.hasData(saveDir);
    if (!resumeWithData) {
      removeTorrentByInfoHash(infoHash);
    }

    for (int attempt = 1; attempt <= 2; attempt++) {
      if (!addTorrent(source, saveDir)) {
        fail(jobId, "Transmission did not accept the torrent");
        return;
      }

      JsonNode torrent = awaitTorrent(infoHash);
      if (torrent == null) {
        fail(jobId, "Transmission did not list the torrent");
        return;
      }

      if (resumeWithData) {
        relocateIfNeeded(torrent, saveDir);
      }

      PollOutcome outcome = poll(jobId, infoHash, saveDir);
      if (outcome == PollOutcome.FINISHED || outcome == PollOutcome.CANCELLED) {
        return;
      }

      if (attempt == 2) {
        fail(jobId, "Transmission reported a finished download but no files were found");
        return;
      }

      log.warn("Retrying job {} with a clean transmission entry", jobId);
      removeTorrentByInfoHash(infoHash);
      ensureDir(saveDir);
      resumeWithData = false;
    }
  }

  /**
   * Tracks a torrent until it completes or stops being resumable.
   *
   * @param jobId the torrent job id.
   * @param infoHash the torrent info hash.
   * @param saveDir the job download directory.
   * @return how the poll ended.
   */
  private PollOutcome poll(UUID jobId, String infoHash, File saveDir) {
    boolean paused = false;

    while (true) {
      TorrentJobEntity job = jobService.findById(jobId).orElse(null);
      if (job == null || job.getStatus() == TorrentStatus.CANCELLED) {
        removeTorrentByInfoHash(infoHash);
        return PollOutcome.CANCELLED;
      }

      JsonNode torrents = getTorrents();
      if (torrents == null) {
        if (!sleep(2000L)) {
          return PollOutcome.CANCELLED;
        }
        continue;
      }
      JsonNode torrent = findTorrent(torrents, infoHash);
      if (torrent == null) {
        return PollOutcome.LOST;
      }

      int torrentId = torrent.path("id").asInt();
      if (job.getStatus() == TorrentStatus.PAUSED) {
        if (!paused) {
          stopTorrent(torrentId);
          paused = true;
        }
        if (!sleep(2000L)) {
          return PollOutcome.CANCELLED;
        }
        continue;
      }
      paused = false;

      if (!isActive(torrent)) {
        startTorrent(torrentId);
        if (!sleep(1000L)) {
          return PollOutcome.CANCELLED;
        }
        JsonNode refreshed = getTorrents();
        if (refreshed != null) {
          JsonNode found = findTorrent(refreshed, infoHash);
          if (found == null) {
            return PollOutcome.LOST;
          }
          torrent = found;
        }
      }

      double progress = torrent.path("percentDone").asDouble(0);
      long downloaded = torrent.path("downloadedEver").asLong(0);
      long total = torrent.path("sizeWhenDone").asLong(0);
      int rate = torrent.path("rateDownload").asInt(0);
      int peers = torrent.path("peersConnected").asInt(0);
      int error = torrent.path("error").asInt(0);

      if (error == ERROR_LOCAL && !TorrentScratch.hasData(saveDir)) {
        log.warn("Job {} lost its local data in {}", jobId, saveDir);
        return PollOutcome.STALE;
      }

      if (progress >= 1.0) {
        if (!TorrentScratch.hasData(saveDir)) {
          log.warn("Job {} finished without local data in {}", jobId, saveDir);
          return PollOutcome.STALE;
        }
        complete(job, downloaded, total, peers);
        return PollOutcome.FINISHED;
      }

      updateProgress(job, progress, downloaded, total, rate, peers);
      log.info(
          "transmission progress for {}: {}% {}",
          jobId,
          String.format("%.1f", progress * 100),
          formatRate(rate));

      if (!sleep(4000L)) {
        return PollOutcome.CANCELLED;
      }
    }
  }

  /**
   * Hands a finished job to the upload service.
   *
   * @param job the torrent job.
   * @param downloaded the downloaded byte count.
   * @param total the wanted byte count.
   * @param peers the connected peer count.
   */
  private void complete(TorrentJobEntity job, long downloaded, long total, int peers) {
    job.setStatus(TorrentStatus.UPLOADING);
    job.setProgress(1.0);
    job.setDownloadedBytes(downloaded);
    if (total > 0) {
      job.setTotalBytes(total);
    }
    job.setDownloadRateBps(0);
    job.setPeersConnected(peers);
    jobService.save(job);
    completionService.complete(job.getId());
  }

  /**
   * Records active download progress on a job.
   *
   * @param job the torrent job.
   * @param progress the fractional progress.
   * @param downloaded the downloaded byte count.
   * @param total the wanted byte count.
   * @param rate the download rate in bytes per second.
   * @param peers the connected peer count.
   */
  private void updateProgress(
      TorrentJobEntity job, double progress, long downloaded, long total, int rate, int peers) {
    job.setStatus(TorrentStatus.DOWNLOADING);
    job.setProgress(progress);
    job.setDownloadedBytes(downloaded);
    if (total > 0) {
      job.setTotalBytes(total);
    }
    job.setDownloadRateBps(rate);
    job.setPeersConnected(peers);
    jobService.save(job);
  }

  /**
   * Adds a torrent to transmission.
   *
   * @param source the magnet URI or .torrent file path.
   * @param saveDir the job download directory.
   * @return true when transmission accepted the torrent.
   */
  private boolean addTorrent(String source, File saveDir) {
    ObjectNode arguments = mapper.createObjectNode();
    arguments.put("filename", source);
    arguments.put("download-dir", saveDir.getAbsolutePath());
    arguments.put("paused", false);
    return rpc.call("torrent-add", arguments) != null;
  }

  /**
   * Waits for transmission to list a torrent for the given hash.
   *
   * @param infoHash the torrent info hash.
   * @return the torrent, or null after the timeout.
   */
  private JsonNode awaitTorrent(String infoHash) {
    for (int attempt = 0; attempt < 15; attempt++) {
      JsonNode torrent = findTorrent(infoHash);
      if (torrent != null) {
        return torrent;
      }
      if (!sleep(1000L)) {
        return null;
      }
    }
    return null;
  }

  /**
   * Points transmission at the job directory and verifies its data.
   *
   * @param torrent the transmission torrent.
   * @param saveDir the job download directory.
   */
  private void relocateIfNeeded(JsonNode torrent, File saveDir) {
    String location = torrent.path("downloadDir").asText("");
    if (location.isBlank() || samePath(location, saveDir)) {
      return;
    }

    int torrentId = torrent.path("id").asInt();
    log.info("Relocating transmission torrent {} from {} to {}", torrentId, location, saveDir);

    ObjectNode setLocation = mapper.createObjectNode();
    ArrayNode setIds = setLocation.putArray("ids");
    setIds.add(torrentId);
    setLocation.put("location", saveDir.getAbsolutePath());
    setLocation.put("move", false);
    rpc.call("torrent-set-location", setLocation);

    ObjectNode verify = mapper.createObjectNode();
    verify.putArray("ids").add(torrentId);
    rpc.call("torrent-verify", verify);
    sleep(1000L);
  }

  /**
   * Lists all torrents with the fields used by the gateway.
   *
   * @return the torrent array, or null when the request fails.
   */
  private JsonNode getTorrents() {
    ObjectNode arguments = mapper.createObjectNode();
    ArrayNode fields = arguments.putArray("fields");
    for (String field : TORRENT_FIELDS) {
      fields.add(field);
    }
    JsonNode response = rpc.call("torrent-get", arguments);
    if (response == null || !response.path("torrents").isArray()) {
      return null;
    }
    return response.path("torrents");
  }

  /**
   * Finds a torrent by its info hash.
   *
   * @param infoHash the torrent info hash.
   * @return the torrent, or null when it is not present.
   */
  private JsonNode findTorrent(String infoHash) {
    return findTorrent(getTorrents(), infoHash);
  }

  /**
   * Finds a torrent in a torrent list.
   *
   * @param torrents the torrent array.
   * @param infoHash the torrent info hash.
   * @return the torrent, or null when it is not present.
   */
  private JsonNode findTorrent(JsonNode torrents, String infoHash) {
    if (torrents == null) {
      return null;
    }
    for (JsonNode torrent : torrents) {
      if (infoHash.equalsIgnoreCase(torrent.path("hashString").asText())) {
        return torrent;
      }
    }
    return null;
  }

  /**
   * Starts a torrent.
   *
   * @param torrentId the transmission torrent id.
   */
  private void startTorrent(int torrentId) {
    ObjectNode arguments = mapper.createObjectNode();
    arguments.putArray("ids").add(torrentId);
    rpc.call("torrent-start", arguments);
  }

  /**
   * Stops a torrent.
   *
   * @param torrentId the transmission torrent id.
   */
  private void stopTorrent(int torrentId) {
    ObjectNode arguments = mapper.createObjectNode();
    arguments.putArray("ids").add(torrentId);
    rpc.call("torrent-stop", arguments);
  }

  /**
   * Reports whether transmission considers a torrent active.
   *
   * @param torrent the transmission torrent.
   * @return true when the torrent is not stopped.
   */
  private boolean isActive(JsonNode torrent) {
    return torrent.path("status").asInt(STATUS_STOPPED) != STATUS_STOPPED;
  }

  /**
   * Compares a stored path with a directory.
   *
   * @param first the stored path.
   * @param second the directory.
   * @return true when both resolve to the same location.
   */
  private boolean samePath(String first, File second) {
    try {
      return new File(first).getCanonicalFile().equals(second.getCanonicalFile());
    } catch (IOException e) {
      return new File(first).getAbsoluteFile().equals(second.getAbsoluteFile());
    }
  }

  /**
   * Creates a directory when it does not exist.
   *
   * @param dir the directory to create.
   */
  private void ensureDir(File dir) {
    if (!dir.isDirectory()) {
      dir.mkdirs();
    }
  }

  /**
   * Reads the info hash stored on a job.
   *
   * @param jobId the torrent job id.
   * @return the info hash, or null when the job is gone.
   */
  private String infoHashFor(UUID jobId) {
    return jobService.findById(jobId).map(TorrentJobEntity::getInfoHash).orElse(null);
  }

  /**
   * Marks a job failed with a readable message.
   *
   * @param jobId the torrent job id.
   * @param message the failure reason.
   */
  private void fail(UUID jobId, String message) {
    jobService
        .findById(jobId)
        .ifPresent(
            job -> {
              job.setStatus(TorrentStatus.FAILED);
              job.setErrorMessage(message);
              jobService.save(job);
            });
    log.warn("Torrent job {} failed: {}", jobId, message);
  }

  /**
   * Sleeps without discarding the interrupt flag.
   *
   * @param millis the sleep duration in milliseconds.
   * @return false when the thread was interrupted.
   */
  private boolean sleep(long millis) {
    try {
      Thread.sleep(millis);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /**
   * Formats a byte rate for logging.
   *
   * @param bytesPerSec the rate in bytes per second.
   * @return the human-readable rate.
   */
  private String formatRate(long bytesPerSec) {
    if (bytesPerSec >= 1_000_000_000) {
      return String.format("%.1f GB/s", bytesPerSec / 1_000_000_000.0);
    }
    if (bytesPerSec >= 1_000_000) {
      return String.format("%.1f MB/s", bytesPerSec / 1_000_000.0);
    }
    if (bytesPerSec >= 1_000) {
      return String.format("%.1f KB/s", bytesPerSec / 1_000.0);
    }
    return bytesPerSec + " B/s";
  }

  /**
   * Describes how a poll ended.
   */
  private enum PollOutcome {
    FINISHED,
    CANCELLED,
    STALE,
    LOST
  }
}

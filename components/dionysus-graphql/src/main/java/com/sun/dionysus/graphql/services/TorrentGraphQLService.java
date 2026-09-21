package com.sun.dionysus.graphql.services;

import com.sun.base.error.MutationException;
import com.sun.dionysus.codegen.types.AddTorrentResponse;
import com.sun.dionysus.codegen.types.CancelTorrentResponse;
import com.sun.dionysus.codegen.types.PauseTorrentResponse;
import com.sun.dionysus.codegen.types.ResumeTorrentResponse;
import com.sun.dionysus.codegen.types.TorrentJob;
import com.sun.dionysus.graphql.mappers.TorrentJobMapper;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import com.sun.dionysus.torrent.MagnetUri;
import com.sun.dionysus.torrent.TorrentClientService;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * GraphQL-layer service backing torrent job queries and mutations.
 */
@Service
public class TorrentGraphQLService {

  @Autowired
  private TorrentJobService torrentJobService;

  @Autowired
  private TorrentJobMapper torrentJobMapper;

  @Autowired
  private TorrentClientService torrentClient;

  /**
   * Looks up a single torrent job by id, with its magnet metadata.
   */
  public TorrentJob locate(String jobId) {
    return torrentJobService
        .findById(UUID.fromString(jobId))
        .map(torrentJobMapper::map)
        .orElse(null);
  }

  /**
   * Lists torrent jobs, optionally narrowed by bucket and status.
   */
  public List<TorrentJob> list(String bucket, String status) {
    TorrentStatus statusFilter = status == null ? null : TorrentStatus.valueOf(status);
    return torrentJobService.findAll().stream()
        .filter(job -> bucket == null || bucket.equals(job.getBucket()))
        .filter(job -> statusFilter == null || statusFilter.equals(job.getStatus()))
        .map(torrentJobMapper::map)
        .toList();
  }

  /**
   * Starts a torrent from a magnet link or base64-encoded .torrent file.
   */
  public AddTorrentResponse addTorrent(String bucket, String path, String magnet, String torrentFileBase64) {
    TorrentJobEntity job;
    if (magnet != null && !magnet.isBlank()) {
      if (MagnetUri.isMagnet(magnet)) {
        job = torrentClient.addFromMagnet(bucket, path, magnet);
      } else if (magnet.startsWith("http://") || magnet.startsWith("https://")) {
        var src = resolveTorrentLink(magnet);
        if (src.isMagnet()) {
          job = torrentClient.addFromMagnet(bucket, path, src.magnet);
        } else {
          job = torrentClient.addFromTorrentFile(bucket, path, src.torrentBytes);
        }
      } else {
        throw new IllegalArgumentException("Not a magnet URI or torrent URL: " + magnet);
      }
    } else if (torrentFileBase64 != null && !torrentFileBase64.isBlank()) {
      job = torrentClient.addFromTorrentFile(bucket, path, Base64.getDecoder().decode(torrentFileBase64));
    } else {
      throw new IllegalArgumentException("addTorrent requires a magnet or torrentFileBase64");
    }
    return AddTorrentResponse.newBuilder()
        .message("addTorrent succeeded")
        .job(torrentJobMapper.map(job))
        .build();
  }

  /**
   * Pauses a running download.
   */
  public PauseTorrentResponse pauseTorrent(String jobId) {
    UUID id = UUID.fromString(jobId);
    torrentClient.pauseJob(id);
    TorrentJob job = torrentJobService.findById(id)
        .map(torrentJobMapper::map)
        .orElseThrow(() -> new MutationException("pauseTorrent failed: torrent job not found: " + jobId));
    return PauseTorrentResponse.newBuilder()
        .message("pauseTorrent succeeded")
        .job(job)
        .build();
  }

  /**
   * Resumes a paused download.
   */
  public ResumeTorrentResponse resumeTorrent(String jobId) {
    UUID id = UUID.fromString(jobId);
    torrentClient.resumeJob(id);
    TorrentJob job = torrentJobService.findById(id)
        .map(torrentJobMapper::map)
        .orElseThrow(() -> new MutationException("resumeTorrent failed: torrent job not found: " + jobId));
    return ResumeTorrentResponse.newBuilder()
        .message("resumeTorrent succeeded")
        .job(job)
        .build();
  }

  /**
   * Cancels a download and clears its scratch files.
   */
  public CancelTorrentResponse cancelTorrent(String jobId) {
    UUID id = UUID.fromString(jobId);
    torrentClient.cancelJob(id);
    TorrentJob job = torrentJobService.findById(id)
        .map(torrentJobMapper::map)
        .orElseThrow(() -> new MutationException("cancelTorrent failed: torrent job not found: " + jobId));
    return CancelTorrentResponse.newBuilder()
        .message("cancelTorrent succeeded")
        .job(job)
        .build();
  }

  /**
   * The resolved source for a torrent, either a magnet URI or the raw
   * .torrent file bytes.
   *
   * @param magnet the magnet URI when the link redirected to a magnet.
   * @param torrentBytes the .torrent bytes when the link served a file.
   */
  private record TorrentSource(String magnet, byte[] torrentBytes) {
    boolean isMagnet() { return magnet != null; }
  }

  /**
   * Follows HTTP redirects on a Jackett download URL.
   */
  private TorrentSource resolveTorrentLink(String urlString) {
    int attempts = 0;
    while (true) {
      try {
        return resolveOnce(urlString);
      } catch (Exception e) {
        if (++attempts >= 2) {
          throw new RuntimeException("Failed to resolve torrent URL: " + urlString, e);
        }
        try { Thread.sleep(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
      }
    }
  }

  /**
   * Single attempt at resolving a torrent link.
   */
  private TorrentSource resolveOnce(String urlString) throws Exception {
    var url = URI.create(urlString).toURL();
    var conn = (HttpURLConnection) url.openConnection();
    conn.setConnectTimeout(10000);
    conn.setReadTimeout(15000);
    conn.setInstanceFollowRedirects(false);
    conn.setRequestMethod("GET");
    int status = conn.getResponseCode();

    if (status >= 300 && status < 400) {
      String location = conn.getHeaderField("Location");
      conn.disconnect();
      if (location == null) {
        throw new RuntimeException("Redirect with no Location");
      }
      if (MagnetUri.isMagnet(location)) {
        return new TorrentSource(location, null);
      }
      if (location.startsWith("http://") || location.startsWith("https://")) {
        return resolveOnce(location);
      }
      throw new RuntimeException("Unknown redirect target: " + location);
    }

    try (var in = conn.getInputStream()) {
      return new TorrentSource(null, in.readAllBytes());
    }
  }
}


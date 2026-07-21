package com.sun.dionysus.graphql.services;

import com.sun.dionysus.codegen.types.TorrentJob;
import com.sun.dionysus.graphql.mappers.TorrentJobMapper;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import com.sun.dionysus.torrent.MagnetUri;
import com.sun.dionysus.torrent.TorrentClientService;
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
  public TorrentJob addTorrent(String bucket, String path, String magnet, String torrentFileBase64) {
    TorrentJobEntity job;
    if (magnet != null && !magnet.isBlank()) {
      if (!MagnetUri.isMagnet(magnet)) {
        throw new IllegalArgumentException("Not a magnet URI: " + magnet);
      }
      job = torrentClient.addFromMagnet(bucket, path, magnet);
    } else if (torrentFileBase64 != null && !torrentFileBase64.isBlank()) {
      job = torrentClient.addFromTorrentFile(bucket, path, Base64.getDecoder().decode(torrentFileBase64));
    } else {
      throw new IllegalArgumentException("addTorrent requires a magnet or torrentFileBase64");
    }
    return torrentJobMapper.map(job);
  }

  /**
   * Pauses a running download.
   */
  public TorrentJob pauseTorrent(String jobId) {
    torrentClient.pauseJob(UUID.fromString(jobId));
    return torrentJobService.findById(UUID.fromString(jobId)).map(torrentJobMapper::map).orElse(null);
  }

  /**
   * Resumes a paused download.
   */
  public TorrentJob resumeTorrent(String jobId) {
    torrentClient.resumeJob(UUID.fromString(jobId));
    return torrentJobService.findById(UUID.fromString(jobId)).map(torrentJobMapper::map).orElse(null);
  }

  /**
   * Cancels a download and clears its scratch files.
   */
  public TorrentJob cancelTorrent(String jobId) {
    torrentClient.cancelJob(UUID.fromString(jobId));
    return torrentJobService.findById(UUID.fromString(jobId)).map(torrentJobMapper::map).orElse(null);
  }
}


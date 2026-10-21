package com.sun.dionysus.graphql.resolvers;
import com.sun.base.audit.annotation.Audited;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.base.error.MutationException;
import com.sun.dionysus.codegen.types.AddTorrentInput;
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
import com.sun.dionysus.torrent.TorrentJobViewService;
import com.sun.dionysus.torrent.search.TorrentSearchResult;
import com.sun.dionysus.torrent.search.TorrentSearchService;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@DgsComponent
public class TorrentDataFetcher {

  private final TorrentJobViewService torrentJobView;
  private final TorrentClientService torrentClient;
  private final TorrentSearchService torrentSearchService;

  public TorrentDataFetcher(
      TorrentJobViewService torrentJobView, TorrentClientService torrentClient,
      TorrentSearchService torrentSearchService) {
    this.torrentJobView = torrentJobView;
    this.torrentClient = torrentClient;
    this.torrentSearchService = torrentSearchService;
  }

  /**
   * Looks up a single torrent job by id, with its magnet metadata.
   */
  @Audited(event = "TORRENTJOB")
  @DgsData(parentType = "FilestoreQueries", field = "torrentJob")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrentJob')")
  public TorrentJob torrentJob(String jobId) {
    return torrentJobView.byId(UUID.fromString(jobId)).orElse(null);
  }

  /**
   * Lists torrent jobs, optionally narrowed by bucket and status.
   */
  @Audited(event = "TORRENTJOBS")
  @DgsData(parentType = "FilestoreQueries", field = "torrentJobs")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrentJobs')")
  public List<TorrentJob> torrentJobs(String bucket, String status) {
    TorrentStatus statusFilter = status == null ? null : TorrentStatus.valueOf(status);
    return torrentJobView.list(bucket, statusFilter);
  }

  @Audited(event = "SEARCHTORRENTS", entity = "Torrent")
  @DgsData(parentType = "FilestoreQueries", field = "searchTorrents")
  @PreAuthorize("@permissions.has('graphql.dionysus.searchTorrents')")
  public List<TorrentSearchResult> searchTorrents(String query) {
    return torrentSearchService.search(query);
  }

  /**
   * Starts a torrent from a magnet link or base64-encoded .torrent file.
   */
  @Audited(event = "ADDTORRENT", entity = "Torrent")
  @DgsData(parentType = "FilestoreMutations", field = "addTorrent")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrent.add')")
  public AddTorrentResponse addTorrent(AddTorrentInput input) {
    String bucket = input.getBucket();
    String path = input.getPath();
    String magnet = input.getMagnet();
    String torrentFileBase64 = input.getTorrentFileBase64();
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
        .job(torrentJobView
            .byId(job.getId())
            .orElseThrow(
                () -> new MutationException("addTorrent failed: job not found after save")))
        .build();
  }

  /**
   * Pauses a running download.
   */
  @Audited(event = "PAUSETORRENT", entity = "Torrent")
  @DgsData(parentType = "FilestoreMutations", field = "pauseTorrent")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrent.pause')")
  public PauseTorrentResponse pauseTorrent(String jobId) {
    UUID id = UUID.fromString(jobId);
    torrentClient.pauseJob(id);
    TorrentJob job = torrentJobView
        .byId(id)
        .orElseThrow(
            () -> new MutationException("pauseTorrent failed: torrent job not found: " + jobId));
    return PauseTorrentResponse.newBuilder()
        .message("pauseTorrent succeeded")
        .job(job)
        .build();
  }

  /**
   * Resumes a paused download.
   */
  @Audited(event = "RESUMETORRENT", entity = "Torrent")
  @DgsData(parentType = "FilestoreMutations", field = "resumeTorrent")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrent.resume')")
  public ResumeTorrentResponse resumeTorrent(String jobId) {
    UUID id = UUID.fromString(jobId);
    torrentClient.resumeJob(id);
    TorrentJob job = torrentJobView
        .byId(id)
        .orElseThrow(
            () -> new MutationException("resumeTorrent failed: torrent job not found: " + jobId));
    return ResumeTorrentResponse.newBuilder()
        .message("resumeTorrent succeeded")
        .job(job)
        .build();
  }

  /**
   * Cancels a download and clears its scratch files.
   */
  @Audited(event = "CANCELTORRENT", entity = "Torrent")
  @DgsData(parentType = "FilestoreMutations", field = "cancelTorrent")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrent.cancel')")
  public CancelTorrentResponse cancelTorrent(String jobId) {
    UUID id = UUID.fromString(jobId);
    torrentClient.cancelJob(id);
    TorrentJob job = torrentJobView
        .byId(id)
        .orElseThrow(
            () -> new MutationException("cancelTorrent failed: torrent job not found: " + jobId));
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
   * Follows HTTP redirects on a Prowlarr download URL.
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

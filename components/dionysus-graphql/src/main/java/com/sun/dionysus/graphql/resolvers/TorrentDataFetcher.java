package com.sun.dionysus.graphql.resolvers;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.sun.dionysus.codegen.types.AddTorrentInput;
import com.sun.dionysus.codegen.types.TorrentJob;
import com.sun.dionysus.graphql.services.TorrentGraphQLService;
import com.sun.dionysus.torrent.search.TorrentSearchResult;
import com.sun.dionysus.torrent.search.TorrentSearchService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

@DgsComponent
public class TorrentDataFetcher {

  @Autowired
  private TorrentGraphQLService torrentGraphQLService;

  @Autowired
  private TorrentSearchService torrentSearchService;

  @DgsData(parentType = "FilestoreQueries", field = "torrentJob")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrentJob')")
  public TorrentJob torrentJob(String jobId) {
    return torrentGraphQLService.locate(jobId);
  }

  @DgsData(parentType = "FilestoreQueries", field = "torrentJobs")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrentJobs')")
  public List<TorrentJob> torrentJobs(String bucket, String status) {
    return torrentGraphQLService.list(bucket, status);
  }

  @DgsData(parentType = "FilestoreQueries", field = "searchTorrents")
  @PreAuthorize("@permissions.has('graphql.dionysus.searchTorrents')")
  public List<TorrentSearchResult> searchTorrents(String query) {
    return torrentSearchService.search(query);
  }

  @DgsData(parentType = "FilestoreMutations", field = "addTorrent")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrent.add')")
  public TorrentJob addTorrent(AddTorrentInput input) {
    return torrentGraphQLService.addTorrent(input.getBucket(), input.getPath(), input.getMagnet(), input.getTorrentFileBase64());
  }

  @DgsData(parentType = "FilestoreMutations", field = "pauseTorrent")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrent.pause')")
  public TorrentJob pauseTorrent(String jobId) {
    return torrentGraphQLService.pauseTorrent(jobId);
  }

  @DgsData(parentType = "FilestoreMutations", field = "resumeTorrent")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrent.resume')")
  public TorrentJob resumeTorrent(String jobId) {
    return torrentGraphQLService.resumeTorrent(jobId);
  }

  @DgsData(parentType = "FilestoreMutations", field = "cancelTorrent")
  @PreAuthorize("@permissions.has('graphql.dionysus.torrent.cancel')")
  public TorrentJob cancelTorrent(String jobId) {
    return torrentGraphQLService.cancelTorrent(jobId);
  }
}

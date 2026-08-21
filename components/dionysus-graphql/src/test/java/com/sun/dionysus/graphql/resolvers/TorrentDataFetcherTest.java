package com.sun.dionysus.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.dionysus.codegen.types.AddTorrentInput;
import com.sun.dionysus.codegen.types.TorrentJob;
import com.sun.dionysus.codegen.types.TorrentJobStatus;
import com.sun.dionysus.graphql.services.TorrentGraphQLService;
import com.sun.dionysus.torrent.search.TorrentSearchResult;
import com.sun.dionysus.torrent.search.TorrentSearchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TorrentDataFetcherTest {

  @Mock private TorrentGraphQLService torrentGraphQLService;
  @Mock private TorrentSearchService torrentSearchService;
  @InjectMocks private TorrentDataFetcher fetcher;

  @Test
  void torrentJob_shouldDelegateToService() {
    TorrentJob job = TorrentJob.newBuilder().id("1").bucket("b").targetKeyPath("k")
        .sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING).infoHash("h")
        .totalBytes(100L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentGraphQLService.locate("1")).thenReturn(job);

    TorrentJob result = fetcher.torrentJob("1");

    assertThat(result).isEqualTo(job);
    verify(torrentGraphQLService).locate("1");
  }

  @Test
  void torrentJobs_shouldDelegateToService() {
    TorrentJob job = TorrentJob.newBuilder().id("1").bucket("b").targetKeyPath("k")
        .sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING).infoHash("h")
        .totalBytes(100L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentGraphQLService.list("b", "DOWNLOADING")).thenReturn(List.of(job));

    List<TorrentJob> result = fetcher.torrentJobs("b", "DOWNLOADING");

    assertThat(result).hasSize(1);
    verify(torrentGraphQLService).list("b", "DOWNLOADING");
  }

  @Test
  void searchTorrents_shouldDelegateToSearchService() {
    TorrentSearchResult r = new TorrentSearchResult("name", 10, 2, "1 MB", 1024L, "2024-01-01", "magnet:?xt=urn:btih:abc");
    when(torrentSearchService.search("ubuntu")).thenReturn(List.of(r));

    List<TorrentSearchResult> result = fetcher.searchTorrents("ubuntu");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("name");
    verify(torrentSearchService).search("ubuntu");
  }

  @Test
  void addTorrent_shouldDelegateToService() {
    AddTorrentInput input = AddTorrentInput.newBuilder().bucket("b").path("p").magnet("magnet:?xt=urn:btih:abc").build();
    TorrentJob job = TorrentJob.newBuilder().id("1").bucket("b").targetKeyPath("p")
        .sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING).infoHash("h")
        .totalBytes(100L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentGraphQLService.addTorrent("b", "p", "magnet:?xt=urn:btih:abc", null)).thenReturn(job);

    TorrentJob result = fetcher.addTorrent(input);

    assertThat(result).isEqualTo(job);
    verify(torrentGraphQLService).addTorrent("b", "p", "magnet:?xt=urn:btih:abc", null);
  }

  @Test
  void pauseTorrent_shouldDelegateToService() {
    TorrentJob job = TorrentJob.newBuilder().id("1").bucket("b").targetKeyPath("k")
        .sourceType("MAGNET").status(TorrentJobStatus.PAUSED).infoHash("h")
        .totalBytes(100L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentGraphQLService.pauseTorrent("1")).thenReturn(job);

    TorrentJob result = fetcher.pauseTorrent("1");

    assertThat(result).isEqualTo(job);
    verify(torrentGraphQLService).pauseTorrent("1");
  }

  @Test
  void resumeTorrent_shouldDelegateToService() {
    TorrentJob job = TorrentJob.newBuilder().id("1").bucket("b").targetKeyPath("k")
        .sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING).infoHash("h")
        .totalBytes(100L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentGraphQLService.resumeTorrent("1")).thenReturn(job);

    TorrentJob result = fetcher.resumeTorrent("1");

    assertThat(result).isEqualTo(job);
    verify(torrentGraphQLService).resumeTorrent("1");
  }

  @Test
  void cancelTorrent_shouldDelegateToService() {
    TorrentJob job = TorrentJob.newBuilder().id("1").bucket("b").targetKeyPath("k")
        .sourceType("MAGNET").status(TorrentJobStatus.CANCELLED).infoHash("h")
        .totalBytes(100L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentGraphQLService.cancelTorrent("1")).thenReturn(job);

    TorrentJob result = fetcher.cancelTorrent("1");

    assertThat(result).isEqualTo(job);
    verify(torrentGraphQLService).cancelTorrent("1");
  }
}

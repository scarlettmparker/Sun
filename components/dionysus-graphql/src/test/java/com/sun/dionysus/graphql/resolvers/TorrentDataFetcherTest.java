package com.sun.dionysus.graphql.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.dionysus.codegen.types.AddTorrentInput;
import com.sun.dionysus.codegen.types.AddTorrentResponse;
import com.sun.dionysus.codegen.types.CancelTorrentResponse;
import com.sun.dionysus.codegen.types.PauseTorrentResponse;
import com.sun.dionysus.codegen.types.ResumeTorrentResponse;
import com.sun.dionysus.codegen.types.TorrentJob;
import com.sun.dionysus.codegen.types.TorrentJobStatus;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.torrent.TorrentClientService;
import com.sun.dionysus.torrent.TorrentJobViewService;
import com.sun.dionysus.torrent.search.TorrentSearchResult;
import com.sun.dionysus.torrent.search.TorrentSearchService;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TorrentDataFetcherTest {

  @Mock private TorrentJobViewService torrentJobView;
  @Mock private TorrentClientService torrentClient;
  @Mock private TorrentSearchService torrentSearchService;

  @InjectMocks private TorrentDataFetcher fetcher;

  @Test
  void torrentJob_shouldReturnMappedJob() {
    UUID id = UUID.randomUUID();
    TorrentJob mapped = TorrentJob.newBuilder().id(id.toString()).bucket("b").targetKeyPath("k")
        .sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING).infoHash("h")
        .totalBytes(100L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentJobView.byId(id)).thenReturn(Optional.of(mapped));

    TorrentJob result = fetcher.torrentJob(id.toString());

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void torrentJob_shouldReturnNullWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(torrentJobView.byId(id)).thenReturn(Optional.empty());

    TorrentJob result = fetcher.torrentJob(id.toString());

    assertThat(result).isNull();
  }

  @Test
  void torrentJobs_shouldReturnAllWhenNoFilter() {
    TorrentJob mapped = TorrentJob.newBuilder().id(UUID.randomUUID().toString()).bucket("b")
        .targetKeyPath("k").sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING).infoHash("h")
        .totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentJobView.list(isNull(), isNull())).thenReturn(List.of(mapped, mapped));

    List<TorrentJob> result = fetcher.torrentJobs(null, null);

    assertThat(result).hasSize(2);
  }

  @Test
  void torrentJobs_shouldFilterByBucketAndStatus() {
    TorrentJob mapped = TorrentJob.newBuilder().id(UUID.randomUUID().toString()).bucket("bucket")
        .targetKeyPath("k").sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING).infoHash("h")
        .totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentJobView.list("bucket", TorrentStatus.DOWNLOADING)).thenReturn(List.of(mapped));

    List<TorrentJob> result = fetcher.torrentJobs("bucket", "DOWNLOADING");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getBucket()).isEqualTo("bucket");
    assertThat(result.get(0).getStatus()).isEqualTo(TorrentJobStatus.DOWNLOADING);
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
  void addTorrent_withMagnet_shouldDelegateToClient() {
    TorrentJobEntity entity = job("b", TorrentStatus.DOWNLOADING);
    TorrentJob mapped = TorrentJob.newBuilder().id(entity.getId().toString()).bucket("b")
        .targetKeyPath("ubuntu.iso").sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING)
        .infoHash("h").totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    String magnet = "magnet:?xt=urn:btih:abc123&dn=ubuntu.iso";
    when(torrentClient.addFromMagnet("b", "path/", magnet)).thenReturn(entity);
    when(torrentJobView.byId(entity.getId())).thenReturn(Optional.of(mapped));

    AddTorrentResponse result = fetcher.addTorrent(AddTorrentInput.newBuilder()
        .bucket("b").path("path/").magnet(magnet).build());

    assertThat(result.getJob()).isEqualTo(mapped);
    verify(torrentClient).addFromMagnet("b", "path/", magnet);
  }

  @Test
  void addTorrent_withTorrentFileBase64_shouldDelegateToClient() {
    TorrentJobEntity entity = job("b", TorrentStatus.DOWNLOADING);
    TorrentJob mapped = TorrentJob.newBuilder().id(entity.getId().toString()).bucket("b")
        .targetKeyPath("k").sourceType("FILE").status(TorrentJobStatus.DOWNLOADING)
        .infoHash("h").totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    byte[] bytes = "fake torrent bytes".getBytes();
    String base64 = Base64.getEncoder().encodeToString(bytes);
    when(torrentClient.addFromTorrentFile(eq("b"), eq("p"), any(byte[].class))).thenReturn(entity);
    when(torrentJobView.byId(entity.getId())).thenReturn(Optional.of(mapped));

    AddTorrentResponse result = fetcher.addTorrent(AddTorrentInput.newBuilder()
        .bucket("b").path("p").torrentFileBase64(base64).build());

    assertThat(result.getJob()).isEqualTo(mapped);
    verify(torrentClient).addFromTorrentFile(eq("b"), eq("p"), any(byte[].class));
  }

  @Test
  void addTorrent_withInvalidMagnet_shouldThrow() {
    AddTorrentInput input = AddTorrentInput.newBuilder().bucket("b").path("p").magnet("not-a-magnet").build();

    assertThatThrownBy(() -> fetcher.addTorrent(input))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addTorrent_withNoInput_shouldThrow() {
    AddTorrentInput input = AddTorrentInput.newBuilder().bucket("b").path("p").build();

    assertThatThrownBy(() -> fetcher.addTorrent(input))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void pauseTorrent_shouldDelegateAndReturnMapped() {
    UUID id = UUID.randomUUID();
    TorrentJob mapped = TorrentJob.newBuilder().id(id.toString()).bucket("b")
        .targetKeyPath("k").sourceType("MAGNET").status(TorrentJobStatus.PAUSED)
        .infoHash("h").totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentJobView.byId(id)).thenReturn(Optional.of(mapped));

    PauseTorrentResponse result = fetcher.pauseTorrent(id.toString());

    verify(torrentClient).pauseJob(id);
    assertThat(result.getJob().getStatus()).isEqualTo(TorrentJobStatus.PAUSED);
  }

  @Test
  void resumeTorrent_shouldDelegateAndReturnMapped() {
    UUID id = UUID.randomUUID();
    TorrentJob mapped = TorrentJob.newBuilder().id(id.toString()).bucket("b")
        .targetKeyPath("k").sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING)
        .infoHash("h").totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentJobView.byId(id)).thenReturn(Optional.of(mapped));

    ResumeTorrentResponse result = fetcher.resumeTorrent(id.toString());

    verify(torrentClient).resumeJob(id);
    assertThat(result.getJob().getStatus()).isEqualTo(TorrentJobStatus.DOWNLOADING);
  }

  @Test
  void cancelTorrent_shouldDelegateAndReturnMapped() {
    UUID id = UUID.randomUUID();
    TorrentJob mapped = TorrentJob.newBuilder().id(id.toString()).bucket("b")
        .targetKeyPath("k").sourceType("MAGNET").status(TorrentJobStatus.CANCELLED)
        .infoHash("h").totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentJobView.byId(id)).thenReturn(Optional.of(mapped));

    CancelTorrentResponse result = fetcher.cancelTorrent(id.toString());

    verify(torrentClient).cancelJob(id);
    assertThat(result.getJob().getStatus()).isEqualTo(TorrentJobStatus.CANCELLED);
  }

  private TorrentJobEntity job(String bucket, TorrentStatus status) {
    TorrentJobEntity e = new TorrentJobEntity();
    e.setId(UUID.randomUUID());
    e.setBucket(bucket);
    e.setTargetKeyPath("k");
    e.setSourceType("MAGNET");
    e.setStatus(status);
    e.setInfoHash("h");
    e.setTotalBytes(0L);
    e.setDownloadedBytes(0L);
    e.setUploadedBytes(0L);
    e.setProgress(0.0);
    return e;
  }
}

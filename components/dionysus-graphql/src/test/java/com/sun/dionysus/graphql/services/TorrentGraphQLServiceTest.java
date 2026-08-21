package com.sun.dionysus.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.dionysus.codegen.types.TorrentJob;
import com.sun.dionysus.codegen.types.TorrentJobStatus;
import com.sun.dionysus.graphql.mappers.TorrentJobMapper;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.TorrentJobService;
import com.sun.dionysus.torrent.TorrentClientService;
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
class TorrentGraphQLServiceTest {

  @Mock private TorrentJobService torrentJobService;
  @Mock private TorrentJobMapper torrentJobMapper;
  @Mock private TorrentClientService torrentClient;

  @InjectMocks private TorrentGraphQLService service;

  @Test
  void locate_shouldReturnMappedJob() {
    UUID id = UUID.randomUUID();
    TorrentJobEntity entity = new TorrentJobEntity();
    entity.setId(id);
    TorrentJob mapped = TorrentJob.newBuilder().id(id.toString()).bucket("b").targetKeyPath("k")
        .sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING).infoHash("h")
        .totalBytes(100L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentJobService.findById(id)).thenReturn(Optional.of(entity));
    when(torrentJobMapper.map(entity)).thenReturn(mapped);

    TorrentJob result = service.locate(id.toString());

    assertThat(result).isEqualTo(mapped);
  }

  @Test
  void locate_shouldReturnNullWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(torrentJobService.findById(id)).thenReturn(Optional.empty());

    TorrentJob result = service.locate(id.toString());

    assertThat(result).isNull();
  }

  @Test
  void list_shouldReturnAllWhenNoFilter() {
    TorrentJobEntity e1 = job("b1", TorrentStatus.DOWNLOADING);
    TorrentJobEntity e2 = job("b2", TorrentStatus.PAUSED);
    when(torrentJobService.findAll()).thenReturn(List.of(e1, e2));
    when(torrentJobMapper.map(any(TorrentJobEntity.class))).thenAnswer(inv -> {
      TorrentJobEntity e = inv.getArgument(0);
      return TorrentJob.newBuilder().id(e.getId().toString()).bucket(e.getBucket())
          .targetKeyPath(e.getTargetKeyPath()).sourceType("MAGNET")
          .status(TorrentJobStatus.valueOf(e.getStatus().name())).infoHash("h")
          .totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    });

    List<TorrentJob> result = service.list(null, null);

    assertThat(result).hasSize(2);
  }

  @Test
  void list_shouldFilterByBucketAndStatus() {
    TorrentJobEntity match = job("bucket", TorrentStatus.DOWNLOADING);
    TorrentJobEntity otherBucket = job("other", TorrentStatus.DOWNLOADING);
    TorrentJobEntity otherStatus = job("bucket", TorrentStatus.PAUSED);
    when(torrentJobService.findAll()).thenReturn(List.of(match, otherBucket, otherStatus));
    when(torrentJobMapper.map(match)).thenReturn(TorrentJob.newBuilder().id(match.getId().toString())
        .bucket("bucket").targetKeyPath("k").sourceType("MAGNET")
        .status(TorrentJobStatus.DOWNLOADING).infoHash("h").totalBytes(0L).downloadedBytes(0L)
        .uploadedBytes(0L).progress(0.0).build());

    List<TorrentJob> result = service.list("bucket", "DOWNLOADING");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getBucket()).isEqualTo("bucket");
    assertThat(result.get(0).getStatus()).isEqualTo(TorrentJobStatus.DOWNLOADING);
  }

  @Test
  void addTorrent_withMagnet_shouldDelegateToClient() {
    TorrentJobEntity entity = job("b", TorrentStatus.DOWNLOADING);
    TorrentJob mapped = TorrentJob.newBuilder().id(entity.getId().toString()).bucket("b")
        .targetKeyPath("ubuntu.iso").sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING)
        .infoHash("h").totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    String magnet = "magnet:?xt=urn:btih:abc123&dn=ubuntu.iso";
    when(torrentClient.addFromMagnet("b", "path/", magnet)).thenReturn(entity);
    when(torrentJobMapper.map(entity)).thenReturn(mapped);

    TorrentJob result = service.addTorrent("b", "path/", magnet, null);

    assertThat(result).isEqualTo(mapped);
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
    when(torrentJobMapper.map(entity)).thenReturn(mapped);

    TorrentJob result = service.addTorrent("b", "p", null, base64);

    assertThat(result).isEqualTo(mapped);
    verify(torrentClient).addFromTorrentFile(eq("b"), eq("p"), any(byte[].class));
  }

  @Test
  void addTorrent_withInvalidMagnet_shouldThrow() {
    assertThatThrownBy(() -> service.addTorrent("b", "p", "not-a-magnet", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addTorrent_withNoInput_shouldThrow() {
    assertThatThrownBy(() -> service.addTorrent("b", "p", null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void pauseTorrent_shouldDelegateAndReturnMapped() {
    UUID id = UUID.randomUUID();
    TorrentJobEntity entity = job("b", TorrentStatus.PAUSED);
    entity.setId(id);
    TorrentJob mapped = TorrentJob.newBuilder().id(id.toString()).bucket("b")
        .targetKeyPath("k").sourceType("MAGNET").status(TorrentJobStatus.PAUSED)
        .infoHash("h").totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentJobService.findById(id)).thenReturn(Optional.of(entity));
    when(torrentJobMapper.map(entity)).thenReturn(mapped);

    TorrentJob result = service.pauseTorrent(id.toString());

    verify(torrentClient).pauseJob(id);
    assertThat(result.getStatus()).isEqualTo(TorrentJobStatus.PAUSED);
  }

  @Test
  void resumeTorrent_shouldDelegateAndReturnMapped() {
    UUID id = UUID.randomUUID();
    TorrentJobEntity entity = job("b", TorrentStatus.DOWNLOADING);
    entity.setId(id);
    TorrentJob mapped = TorrentJob.newBuilder().id(id.toString()).bucket("b")
        .targetKeyPath("k").sourceType("MAGNET").status(TorrentJobStatus.DOWNLOADING)
        .infoHash("h").totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentJobService.findById(id)).thenReturn(Optional.of(entity));
    when(torrentJobMapper.map(entity)).thenReturn(mapped);

    TorrentJob result = service.resumeTorrent(id.toString());

    verify(torrentClient).resumeJob(id);
    assertThat(result.getStatus()).isEqualTo(TorrentJobStatus.DOWNLOADING);
  }

  @Test
  void cancelTorrent_shouldDelegateAndReturnMapped() {
    UUID id = UUID.randomUUID();
    TorrentJobEntity entity = job("b", TorrentStatus.CANCELLED);
    entity.setId(id);
    TorrentJob mapped = TorrentJob.newBuilder().id(id.toString()).bucket("b")
        .targetKeyPath("k").sourceType("MAGNET").status(TorrentJobStatus.CANCELLED)
        .infoHash("h").totalBytes(0L).downloadedBytes(0L).uploadedBytes(0L).progress(0.0).build();
    when(torrentJobService.findById(id)).thenReturn(Optional.of(entity));
    when(torrentJobMapper.map(entity)).thenReturn(mapped);

    TorrentJob result = service.cancelTorrent(id.toString());

    verify(torrentClient).cancelJob(id);
    assertThat(result.getStatus()).isEqualTo(TorrentJobStatus.CANCELLED);
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

package com.sun.dionysus.torrent;

import com.sun.dionysus.model.MagnetDetailEntity;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import com.sun.dionysus.service.torrent.MagnetDetailService;
import com.sun.dionysus.service.torrent.TorrentJobService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TorrentClientService.
 */
@ExtendWith(MockitoExtension.class)
class TorrentClientServiceTest {

  @TempDir Path tempDir;

  @Mock private TorrentAlertDispatcher dispatcher;
  @Mock private TorrentJobService jobService;
  @Mock private MagnetDetailService magnetService;
  @Mock private TorrentJobRegistry registry;
  @Mock private TorrentDownloadGateway gateway;
  @Mock private WebTorrentGateway webGateway;
  @Mock private Aria2Gateway aria2Gateway;
  @Mock private TransmissionGateway transmissionGateway;
  @Mock private TorrentClientProperties properties;

  @InjectMocks private TorrentClientService service;

  @Test
  void resumeExistingJob_usesCanonicalDirWhenStoredDataIsMissing() throws IOException {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    ReflectionTestUtils.setField(service, "scratchRoot", root.toFile());
    UUID jobId = UUID.randomUUID();
    TorrentJobEntity job = magnetJob(jobId, tempDir.resolve("missing").toString());
    when(jobService.findById(jobId)).thenReturn(Optional.of(job));
    when(jobService.save(any(TorrentJobEntity.class))).thenAnswer(i -> i.getArgument(0));

    service.resumeExistingJob(jobId);

    File expected = root.resolve(jobId.toString()).toFile();
    assertThat(expected).isDirectory();
    assertThat(job.getScratchPath()).isEqualTo(expected.getAbsolutePath());
    assertThat(job.getStatus()).isEqualTo(TorrentStatus.DOWNLOADING);
    verify(transmissionGateway)
        .resumeMagnet(eq(jobId), eq("magnet:?xt=urn:btih:abc"), eq(expected));
  }

  @Test
  void resumeExistingJob_keepsStoredDirWhenItHasData() throws IOException {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    Path stored = Files.createDirectory(root.resolve("stored"));
    Files.writeString(stored.resolve("movie.mp4"), "payload");
    ReflectionTestUtils.setField(service, "scratchRoot", root.toFile());
    UUID jobId = UUID.randomUUID();
    TorrentJobEntity job = magnetJob(jobId, stored.toString());
    when(jobService.findById(jobId)).thenReturn(Optional.of(job));
    when(jobService.save(any(TorrentJobEntity.class))).thenAnswer(i -> i.getArgument(0));

    service.resumeExistingJob(jobId);

    assertThat(job.getScratchPath()).isEqualTo(stored.toAbsolutePath().toString());
    verify(transmissionGateway)
        .resumeMagnet(eq(jobId), eq("magnet:?xt=urn:btih:abc"), eq(stored.toFile()));
  }

  @Test
  void resumeExistingJob_usesTorrentFileForFileJobs() throws IOException {
    Path root = Files.createDirectory(tempDir.resolve("root"));
    Path stored = Files.createDirectory(root.resolve("stored"));
    Files.writeString(stored.resolve("source.torrent"), "metadata");
    Files.writeString(stored.resolve("movie.mp4"), "payload");
    ReflectionTestUtils.setField(service, "scratchRoot", root.toFile());
    UUID jobId = UUID.randomUUID();
    TorrentJobEntity job = magnetJob(jobId, stored.toString());
    job.setSourceType("FILE");
    when(jobService.findById(jobId)).thenReturn(Optional.of(job));
    when(jobService.save(any(TorrentJobEntity.class))).thenAnswer(i -> i.getArgument(0));

    service.resumeExistingJob(jobId);

    verify(transmissionGateway)
        .resumeTorrentFile(
            eq(jobId), eq(stored.resolve("source.torrent").toFile()), eq(stored.toFile()));
  }

  private TorrentJobEntity magnetJob(UUID jobId, String scratchPath) {
    MagnetDetailEntity detail = new MagnetDetailEntity();
    detail.setSourceUri("magnet:?xt=urn:btih:abc");

    TorrentJobEntity job = new TorrentJobEntity();
    job.setId(jobId);
    job.setInfoHash("abc");
    job.setSourceType("MAGNET");
    job.setScratchPath(scratchPath);
    job.setMagnetDetail(detail);
    return job;
  }
}

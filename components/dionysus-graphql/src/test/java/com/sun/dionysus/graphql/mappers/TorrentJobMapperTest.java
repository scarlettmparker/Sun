package com.sun.dionysus.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.dionysus.codegen.types.TorrentJob;
import com.sun.dionysus.codegen.types.TorrentJobStatus;
import com.sun.dionysus.model.MagnetDetailEntity;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TorrentJobMapperTest {

  private final MagnetDetailMapper magnetDetailMapper = new MagnetDetailMapper();
  private final TorrentJobMapper mapper = new TorrentJobMapper(magnetDetailMapper);

  @Test
  void map_shouldMapAllFields() {
    UUID jobId = UUID.randomUUID();
    UUID magnetId = UUID.randomUUID();
    MagnetDetailEntity magnet = new MagnetDetailEntity();
    magnet.setId(magnetId);
    magnet.setInfoHash("abc123");
    magnet.setDisplayName("ubuntu.iso");
    magnet.setTotalSize(500L);
    magnet.setPrivate(false);

    TorrentJobEntity entity = new TorrentJobEntity();
    entity.setId(jobId);
    entity.setBucket("my-bucket");
    entity.setTargetKeyPath("ubuntu.iso");
    entity.setSourceType("MAGNET");
    entity.setStatus(TorrentStatus.DOWNLOADING);
    entity.setInfoHash("abc123");
    entity.setTotalBytes(500L);
    entity.setDownloadedBytes(250L);
    entity.setUploadedBytes(10L);
    entity.setProgress(0.5);
    entity.setDownloadRateBps(1000);
    entity.setUploadRateBps(200);
    entity.setPeersConnected(12);
    entity.setSeedsConnected(3);
    entity.setEtaSeconds(3600L);
    entity.setErrorMessage("oops");
    entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
    entity.setCompletedAt(LocalDateTime.of(2024, 1, 2, 10, 0));
    entity.setMagnetDetail(magnet);

    TorrentJob result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(jobId.toString());
    assertThat(result.getBucket()).isEqualTo("my-bucket");
    assertThat(result.getTargetKeyPath()).isEqualTo("ubuntu.iso");
    assertThat(result.getSourceType()).isEqualTo("MAGNET");
    assertThat(result.getStatus()).isEqualTo(TorrentJobStatus.DOWNLOADING);
    assertThat(result.getInfoHash()).isEqualTo("abc123");
    assertThat(result.getTotalBytes()).isEqualTo(500L);
    assertThat(result.getDownloadedBytes()).isEqualTo(250L);
    assertThat(result.getUploadedBytes()).isEqualTo(10L);
    assertThat(result.getProgress()).isEqualTo(0.5);
    assertThat(result.getDownloadRateBps()).isEqualTo(1000);
    assertThat(result.getUploadRateBps()).isEqualTo(200);
    assertThat(result.getPeersConnected()).isEqualTo(12);
    assertThat(result.getSeedsConnected()).isEqualTo(3);
    assertThat(result.getEtaSeconds()).isEqualTo(3600L);
    assertThat(result.getErrorMessage()).isEqualTo("oops");
    assertThat(result.getCreatedAt()).isEqualTo("2024-01-01T10:00");
    assertThat(result.getCompletedAt()).isEqualTo("2024-01-02T10:00");
    assertThat(result.getMagnetDetail()).isNotNull();
    assertThat(result.getMagnetDetail().getId()).isEqualTo(magnetId.toString());
    assertThat(result.getMagnetDetail().getInfoHash()).isEqualTo("abc123");
  }

  @Test
  void map_withMinimalFields_shouldMapRequiredOnly() {
    UUID jobId = UUID.randomUUID();
    TorrentJobEntity entity = new TorrentJobEntity();
    entity.setId(jobId);
    entity.setBucket("bucket");
    entity.setTargetKeyPath("file.txt");
    entity.setSourceType("FILE");
    entity.setStatus(TorrentStatus.QUEUED);
    entity.setInfoHash("hash");
    entity.setTotalBytes(0L);
    entity.setDownloadedBytes(0L);
    entity.setUploadedBytes(0L);
    entity.setProgress(0.0);

    TorrentJob result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(jobId.toString());
    assertThat(result.getBucket()).isEqualTo("bucket");
    assertThat(result.getStatus()).isEqualTo(TorrentJobStatus.QUEUED);
    assertThat(result.getDownloadRateBps()).isNull();
    assertThat(result.getUploadRateBps()).isNull();
    assertThat(result.getPeersConnected()).isNull();
    assertThat(result.getSeedsConnected()).isNull();
    assertThat(result.getEtaSeconds()).isNull();
    assertThat(result.getErrorMessage()).isNull();
    assertThat(result.getCreatedAt()).isNull();
    assertThat(result.getCompletedAt()).isNull();
    assertThat(result.getMagnetDetail()).isNull();
  }

}

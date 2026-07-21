package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.model.KeyDetailEntity;
import com.sun.dionysus.model.MagnetDetailEntity;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sun.dionysus.codegen.types.KeyEntry;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.S3Object;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class KeyEntryMapperTest {

  private KeyEntryMapper keyEntryMapper = new KeyEntryMapper();

  @Test
  void mapDirectory_shouldCreateDirectoryEntry() {
    CommonPrefix prefix = CommonPrefix.builder().prefix("folder/").build();

    KeyEntry result = keyEntryMapper.mapDirectory(prefix);

    assertThat(result.getKey()).isEqualTo("folder/");
    assertThat(result.getIsDirectory()).isTrue();
    assertThat(result.getSize()).isEqualTo(0);
    assertThat(result.getLastModified()).isNull();
  }

  @Test
  void mapDirectory_withKeyDetail_enrichesEntry() {
    CommonPrefix prefix = CommonPrefix.builder().prefix("folder/").build();
    KeyDetailEntity detail = new KeyDetailEntity();
    detail.setName("My Folder");
    detail.setDescription("A folder");

    KeyEntry result = keyEntryMapper.mapDirectory(prefix, detail);

    assertThat(result.getKey()).isEqualTo("folder/");
    assertThat(result.getIsDirectory()).isTrue();
    assertThat(result.getName()).isEqualTo("My Folder");
    assertThat(result.getDescription()).isEqualTo("A folder");
  }

  @Test
  void mapFile_shouldCreateFileEntry() {
    S3Object object = S3Object.builder()
        .key("folder/file.txt")
        .size(123L)
        .lastModified(Instant.parse("2024-01-01T10:00:00Z"))
        .build();

    KeyEntry result = keyEntryMapper.mapFile(object);

    assertThat(result.getKey()).isEqualTo("folder/file.txt");
    assertThat(result.getIsDirectory()).isFalse();
    assertThat(result.getSize()).isEqualTo(123);
    assertThat(result.getLastModified()).isEqualTo("2024-01-01T10:00:00Z");
  }

  @Test
  void mapFile_withKeyDetail_enrichesEntry() {
    S3Object object = S3Object.builder()
        .key("folder/file.txt")
        .size(123L)
        .build();
    KeyDetailEntity detail = new KeyDetailEntity();
    detail.setName("Important File");
    detail.setDescription("Document");

    KeyEntry result = keyEntryMapper.mapFile(object, detail);

    assertThat(result.getKey()).isEqualTo("folder/file.txt");
    assertThat(result.getIsDirectory()).isFalse();
    assertThat(result.getName()).isEqualTo("Important File");
    assertThat(result.getDescription()).isEqualTo("Document");
  }

  @Test
  void mapTorrentJob_buildsEntryWithNestedDownload() {
    MagnetDetailEntity magnet = new MagnetDetailEntity();
    magnet.setDisplayName("ubuntu.iso");

    TorrentJobEntity job = new TorrentJobEntity();
    job.setId(UUID.randomUUID());
    job.setBucket("bucket");
    job.setTargetKeyPath("ubuntu.iso");
    job.setTotalBytes(500L);
    job.setProgress(0.5);
    job.setStatus(TorrentStatus.DOWNLOADING);
    job.setDownloadRateBps(1000);
    job.setPeersConnected(12);
    job.setMagnetDetail(magnet);

    KeyEntry result = keyEntryMapper.mapTorrentJob(job);

    assertThat(result.getKey()).isEqualTo("ubuntu.iso");
    assertThat(result.getIsDirectory()).isFalse();
    assertThat(result.getSize()).isEqualTo(500);
    assertThat(result.getName()).isEqualTo("ubuntu.iso");
    assertThat(result.getTorrent()).isNotNull();
    assertThat(result.getTorrent().getStatus()).isEqualTo("DOWNLOADING");
    assertThat(result.getTorrent().getProgress()).isEqualTo(0.5);
    assertThat(result.getTorrent().getDownloadRateBps()).isEqualTo(1000);
    assertThat(result.getTorrent().getPeersConnected()).isEqualTo(12);
    assertThat(result.getTorrent().getMagnetDetailId()).isEqualTo(magnet.getId() == null ? null : magnet.getId().toString());
  }

  @Test
  void mergeTorrentJob_attachesDownloadToExistingEntry() {
    S3Object object = S3Object.builder().key("ubuntu.iso").size(500L).build();
    KeyEntry entry = keyEntryMapper.mapFile(object);

    TorrentJobEntity job = new TorrentJobEntity();
    job.setId(UUID.randomUUID());
    job.setTargetKeyPath("ubuntu.iso");
    job.setTotalBytes(500L);
    job.setProgress(0.75);
    job.setStatus(TorrentStatus.UPLOADING);

    keyEntryMapper.mergeTorrentJob(entry, job);

    assertThat(entry.getTorrent()).isNotNull();
    assertThat(entry.getTorrent().getStatus()).isEqualTo("UPLOADING");
    assertThat(entry.getTorrent().getProgress()).isEqualTo(0.75);
  }
}


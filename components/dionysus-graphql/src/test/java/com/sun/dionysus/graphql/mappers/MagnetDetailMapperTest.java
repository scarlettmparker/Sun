package com.sun.dionysus.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.dionysus.codegen.types.MagnetDetail;
import com.sun.dionysus.model.MagnetDetailEntity;
import com.sun.dionysus.model.TorrentFileEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MagnetDetailMapperTest {

  private final MagnetDetailMapper mapper = new MagnetDetailMapper();

  @Test
  void map_shouldMapAllFields() {
    UUID id = UUID.randomUUID();
    MagnetDetailEntity entity = new MagnetDetailEntity();
    entity.setId(id);
    entity.setInfoHash("abc123hash");
    entity.setDisplayName("ubuntu.iso");
    entity.setTotalSize(123456L);
    entity.setPrivate(false);
    entity.setInfoHashVersion("1");
    entity.setSourceUri("magnet:?xt=urn:btih:abc123hash");
    entity.setComment("comment");
    entity.setCreatedByTorrent("transmission");
    entity.setPieceLength(262144L);
    entity.setPieceCount(10);
    entity.setTrackers(List.of("http://tracker.example.com/announce"));

    TorrentFileEntity file = new TorrentFileEntity();
    file.setIndexInTorrent(0);
    file.setPath("ubuntu.iso");
    file.setSize(123456L);
    entity.setFiles(List.of(file));

    MagnetDetail result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getInfoHash()).isEqualTo("abc123hash");
    assertThat(result.getDisplayName()).isEqualTo("ubuntu.iso");
    assertThat(result.getTotalSize()).isEqualTo(123456L);
    assertThat(result.getIsPrivate()).isFalse();
    assertThat(result.getInfoHashVersion()).isEqualTo("1");
    assertThat(result.getSourceUri()).isEqualTo("magnet:?xt=urn:btih:abc123hash");
    assertThat(result.getComment()).isEqualTo("comment");
    assertThat(result.getCreatedBy()).isEqualTo("transmission");
    assertThat(result.getPieceLength()).isEqualTo(262144L);
    assertThat(result.getPieceCount()).isEqualTo(10);
    assertThat(result.getTrackers()).containsExactly("http://tracker.example.com/announce");
    assertThat(result.getFiles()).hasSize(1);
    assertThat(result.getFiles().get(0).getIndexInTorrent()).isEqualTo(0);
    assertThat(result.getFiles().get(0).getPath()).isEqualTo("ubuntu.iso");
    assertThat(result.getFiles().get(0).getSize()).isEqualTo(123456L);
  }

  @Test
  void map_withMinimalFields_shouldMapRequiredOnly() {
    UUID id = UUID.randomUUID();
    MagnetDetailEntity entity = new MagnetDetailEntity();
    entity.setId(id);
    entity.setInfoHash("hash2");
    entity.setDisplayName("minimal");
    entity.setTotalSize(0L);
    entity.setPrivate(true);

    MagnetDetail result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getInfoHash()).isEqualTo("hash2");
    assertThat(result.getDisplayName()).isEqualTo("minimal");
    assertThat(result.getTotalSize()).isEqualTo(0L);
    assertThat(result.getIsPrivate()).isTrue();
    assertThat(result.getInfoHashVersion()).isNull();
    assertThat(result.getSourceUri()).isNull();
    assertThat(result.getComment()).isNull();
    assertThat(result.getCreatedBy()).isNull();
    assertThat(result.getPieceLength()).isNull();
    assertThat(result.getPieceCount()).isNull();
    assertThat(result.getTrackers()).isNull();
    assertThat(result.getFiles()).isNull();
  }

}

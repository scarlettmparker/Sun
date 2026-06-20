package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.graphql.models.KeyDetailEntity;
import com.sun.dionysus.graphql.models.Status;
import com.sun.dionysus.codegen.types.KeyDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for KeyDetailMapper.
 */
@ExtendWith(MockitoExtension.class)
class KeyDetailMapperTest {

  private KeyDetailMapper mapper = new KeyDetailMapper();

  @Test
  void map_withKeyDetail_mapsAllFields() {
    KeyDetailEntity entity = new KeyDetailEntity();
    entity.setBucket("my-bucket");
    entity.setKeyPath("documents/report.pdf");
    entity.setName("Quarterly Report");
    entity.setDescription("Q4 financial summary");
    entity.setStatus(Status.ACTIVE);
    entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30));
    entity.setLastUpdatedAt(LocalDateTime.of(2024, 3, 1, 14, 0));
    entity.setArchivedAt(null);

    KeyDetail result = mapper.map(entity);

    assertThat(result).isNotNull();
    assertThat(result.getBucket()).isEqualTo("my-bucket");
    assertThat(result.getKeyPath()).isEqualTo("documents/report.pdf");
    assertThat(result.getName()).isEqualTo("Quarterly Report");
    assertThat(result.getDescription()).isEqualTo("Q4 financial summary");
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
    assertThat(result.getCreatedAt()).isEqualTo("2024-01-15T10:30");
    assertThat(result.getLastUpdatedAt()).isEqualTo("2024-03-01T14:00");
    assertThat(result.getArchivedAt()).isNull();
  }

  @Test
  void map_withArchivedEntity_mapsArchivedAt() {
    KeyDetailEntity entity = new KeyDetailEntity();
    entity.setBucket("b");
    entity.setKeyPath("old/file.txt");
    entity.setStatus(Status.ARCHIVED);
    entity.setCreatedAt(LocalDateTime.of(2023, 6, 1, 0, 0));
    entity.setLastUpdatedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
    entity.setArchivedAt(LocalDateTime.of(2024, 2, 15, 9, 0));

    KeyDetail result = mapper.map(entity);

    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo("ARCHIVED");
    assertThat(result.getArchivedAt()).isEqualTo("2024-02-15T09:00");
  }

  @Test
  void map_withNullTimestamps_mapsToNull() {
    KeyDetailEntity entity = new KeyDetailEntity();
    entity.setBucket("b");
    entity.setKeyPath("k");
    entity.setStatus(Status.ACTIVE);

    KeyDetail result = mapper.map(entity);

    assertThat(result).isNotNull();
    assertThat(result.getCreatedAt()).isNull();
    assertThat(result.getLastUpdatedAt()).isNull();
    assertThat(result.getArchivedAt()).isNull();
  }

  @Test
  void map_withNull_returnsNull() {
    KeyDetail result = mapper.map(null);

    assertThat(result).isNull();
  }
}
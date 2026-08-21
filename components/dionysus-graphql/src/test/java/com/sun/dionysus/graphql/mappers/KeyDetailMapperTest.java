package com.sun.dionysus.graphql.mappers;

import com.sun.dionysus.model.KeyDetailEntity;
import com.sun.dionysus.model.enums.Status;
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
    entity.setId(UUID.randomUUID());
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
  void map_withNull_returnsNull() {
    KeyDetail result = mapper.map(null);

    assertThat(result).isNull();
  }
}
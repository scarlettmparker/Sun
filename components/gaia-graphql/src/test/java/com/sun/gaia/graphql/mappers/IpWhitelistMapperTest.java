package com.sun.gaia.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.gaia.codegen.types.IpWhitelistEntry;
import com.sun.gaia.model.IpWhitelistEntryEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IpWhitelistMapperTest {

  private final IpWhitelistMapper mapper = new IpWhitelistMapper();

  @Test
  void map_singleEntityMapsAllFields() {
    UUID id = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime lastUpdatedAt = LocalDateTime.now();

    IpWhitelistEntryEntity entity = new IpWhitelistEntryEntity();
    entity.setId(id);
    entity.setPattern("10.0.0.1");
    entity.setDescription("test");
    entity.setEnabled(true);
    entity.setImmutable(false);
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(lastUpdatedAt);

    IpWhitelistEntry result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getPattern()).isEqualTo("10.0.0.1");
    assertThat(result.getDescription()).isEqualTo("test");
    assertThat(result.getEnabled()).isTrue();
    assertThat(result.getImmutable()).isFalse();
    assertThat(result.getCreatedAt()).isNotNull();
    assertThat(result.getUpdatedAt()).isNotNull();
  }

  @Test
  void map_listMapsAllEntities() {
    IpWhitelistEntryEntity e1 = new IpWhitelistEntryEntity();
    e1.setId(UUID.randomUUID());
    e1.setPattern("10.0.0.1");
    e1.setEnabled(true);

    IpWhitelistEntryEntity e2 = new IpWhitelistEntryEntity();
    e2.setId(UUID.randomUUID());
    e2.setPattern("10.0.0.2");
    e2.setEnabled(false);

    List<IpWhitelistEntry> result = mapper.map(List.of(e1, e2));

    assertThat(result).hasSize(2);
  }
}

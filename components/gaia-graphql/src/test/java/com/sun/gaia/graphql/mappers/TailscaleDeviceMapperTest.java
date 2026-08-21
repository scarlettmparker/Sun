package com.sun.gaia.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.gaia.codegen.types.DeviceStatus;
import com.sun.gaia.codegen.types.TailscaleDevice;
import com.sun.gaia.model.TailscaleDeviceEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TailscaleDeviceMapperTest {

  private final TailscaleDeviceMapper mapper = new TailscaleDeviceMapper();

  @Test
  void map_singleEntityMapsAllFields() {
    UUID id = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime lastUpdatedAt = LocalDateTime.now();

    TailscaleDeviceEntity entity = new TailscaleDeviceEntity();
    entity.setId(id);
    entity.setHeadscaleId(123L);
    entity.setName("node1");
    entity.setStatus(com.sun.gaia.model.enums.DeviceStatus.ACTIVE);
    entity.setOnline(true);
    entity.setIpv4("100.64.0.1");
    entity.setExpiredAt(null);
    entity.setLastSeen("2024-01-01");
    entity.setCreatedAt(createdAt);
    entity.setLastUpdatedAt(lastUpdatedAt);

    TailscaleDevice result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getHeadscaleId()).isEqualTo(123L);
    assertThat(result.getName()).isEqualTo("node1");
    assertThat(result.getStatus()).isEqualTo(DeviceStatus.ACTIVE);
    assertThat(result.getOnline()).isTrue();
    assertThat(result.getIpv4()).isEqualTo("100.64.0.1");
    assertThat(result.getExpiredAt()).isNull();
    assertThat(result.getLastSeen()).isEqualTo("2024-01-01");
    assertThat(result.getCreatedAt()).isNotNull();
    assertThat(result.getUpdatedAt()).isNotNull();
  }

  @Test
  void map_listMapsAllEntities() {
    TailscaleDeviceEntity e1 = new TailscaleDeviceEntity();
    e1.setId(UUID.randomUUID());
    e1.setHeadscaleId(1L);
    e1.setName("node1");
    e1.setStatus(com.sun.gaia.model.enums.DeviceStatus.ACTIVE);
    e1.setOnline(true);

    TailscaleDeviceEntity e2 = new TailscaleDeviceEntity();
    e2.setId(UUID.randomUUID());
    e2.setHeadscaleId(2L);
    e2.setName("node2");
    e2.setStatus(com.sun.gaia.model.enums.DeviceStatus.EXPIRED);
    e2.setOnline(false);

    List<TailscaleDevice> result = mapper.map(List.of(e1, e2));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getStatus()).isEqualTo(DeviceStatus.ACTIVE);
    assertThat(result.get(1).getStatus()).isEqualTo(DeviceStatus.EXPIRED);
  }
}

package com.sun.gaia.graphql.mappers;

import java.time.ZoneOffset;
import com.sun.gaia.codegen.types.DeviceStatus;
import com.sun.gaia.codegen.types.TailscaleDevice;
import com.sun.gaia.model.TailscaleDeviceEntity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Maps TailscaleDeviceEntity domain objects to GraphQL TailscaleDevice types.
 */
@Component
public class TailscaleDeviceMapper {

    /**
     * Maps a single entity to its GraphQL representation.
     */
    public TailscaleDevice map(TailscaleDeviceEntity entity) {
        TailscaleDevice.Builder builder = TailscaleDevice.newBuilder()
                .id(entity.getId().toString())
                .headscaleId(entity.getHeadscaleId())
                .name(entity.getName())
                .status(DeviceStatus.valueOf(entity.getStatus().name()))
                .online(entity.isOnline());

        if (entity.getIpv4() != null) {
            builder.ipv4(entity.getIpv4());
        }
        if (entity.getExpiredAt() != null) {
            builder.expiredAt(entity.getExpiredAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getLastSeen() != null) {
            builder.lastSeen(entity.getLastSeen());
        }
        if (entity.getCreatedAt() != null) {
            builder.createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getLastUpdatedAt() != null) {
            builder.updatedAt(entity.getLastUpdatedAt().atOffset(ZoneOffset.UTC));
        }

        return builder.build();
    }

    /**
     * Maps a list of entities to their GraphQL representations.
     */
    public List<TailscaleDevice> map(List<TailscaleDeviceEntity> entities) {
        return entities.stream()
                .map(this::map)
                .collect(Collectors.toList());
    }
}

package com.sun.gaia.graphql.mappers;

import com.sun.gaia.codegen.types.IpWhitelistEntry;
import com.sun.gaia.model.IpWhitelistEntryEntity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Converts between IP whitelist entities and their GraphQL representation.
 */
@Component
public class IpWhitelistMapper {

    /**
     * Maps a single entity to a GraphQL type.
     *
     * @param entity the persisted entity.
     * @return the GraphQL IpWhitelistEntry.
     */
    public IpWhitelistEntry map(IpWhitelistEntryEntity entity) {
        IpWhitelistEntry.Builder builder = IpWhitelistEntry.newBuilder()
                .id(entity.getId().toString())
                .pattern(entity.getPattern())
                .enabled(entity.isEnabled());

        if (entity.getDescription() != null) {
            builder.description(entity.getDescription());
        }
        if (entity.getCreatedAt() != null) {
            builder.createdAt(entity.getCreatedAt());
        }
        if (entity.getLastUpdatedAt() != null) {
            builder.updatedAt(entity.getLastUpdatedAt());
        }

        return builder.build();
    }

    /**
     * Maps a list of entities.
     *
     * @param entities the persisted entities.
     * @return the list of GraphQL types.
     */
    public List<IpWhitelistEntry> map(List<IpWhitelistEntryEntity> entities) {
        return entities.stream()
                .map(this::map)
                .collect(Collectors.toList());
    }
}

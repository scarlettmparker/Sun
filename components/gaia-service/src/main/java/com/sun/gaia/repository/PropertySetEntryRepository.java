package com.sun.gaia.repository;

import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.model.enums.EntryStatus;
import com.sun.base.repository.BaseRepository;
import java.util.List;
import java.util.Optional;

public interface PropertySetEntryRepository extends BaseRepository<PropertySetEntryEntity> {

  Optional<PropertySetEntryEntity> findByOwnerKeyAndPropertySetAndEntryName(
      String ownerKey, String propertySet, String entryName);

  List<PropertySetEntryEntity> findByOwnerKeyAndPropertySetAndStatus(
      String ownerKey, String propertySet, EntryStatus status);

  List<PropertySetEntryEntity> findByOwnerKeyAndPropertySetAndConfigurable(
      String ownerKey, String propertySet, boolean configurable);
}

package com.sun.gaia.repository;

import com.sun.gaia.model.PropertySetSchemaEntity;
import com.sun.base.repository.BaseRepository;
import java.util.List;
import java.util.Optional;

public interface PropertySetSchemaRepository extends BaseRepository<PropertySetSchemaEntity> {

  Optional<PropertySetSchemaEntity> findByOwnerKeyAndName(String ownerKey, String name);

  List<PropertySetSchemaEntity> findByOwnerKeyAndConfigurableTrue(String ownerKey);
}

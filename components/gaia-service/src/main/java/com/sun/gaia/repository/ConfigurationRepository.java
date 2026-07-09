package com.sun.gaia.repository;

import com.sun.gaia.model.ConfigurationEntity;
import com.sun.base.repository.BaseRepository;
import java.util.List;
import java.util.Optional;

public interface ConfigurationRepository extends BaseRepository<ConfigurationEntity> {

  Optional<ConfigurationEntity> findByName(String name);

  List<ConfigurationEntity> findByEnabledTrue();
}

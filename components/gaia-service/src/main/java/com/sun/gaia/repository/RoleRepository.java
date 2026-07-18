package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.RoleEntity;
import java.util.Optional;

public interface RoleRepository extends BaseRepository<RoleEntity> {

  Optional<RoleEntity> findByName(String name);
}

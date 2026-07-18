package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.RolePermissionEntity;
import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends BaseRepository<RolePermissionEntity> {

  List<RolePermissionEntity> findByRoleId(UUID roleId);
}

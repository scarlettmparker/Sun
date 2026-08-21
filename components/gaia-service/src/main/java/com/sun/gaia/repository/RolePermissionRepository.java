package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.RolePermissionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

public interface RolePermissionRepository extends BaseRepository<RolePermissionEntity> {

  List<RolePermissionEntity> findByRoleId(UUID roleId);

  void deleteByRoleId(UUID roleId);

  @Query("SELECT DISTINCT e.permission FROM RolePermissionEntity e")
  List<String> findDistinctPermissions();
}

package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.AccountPermissionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

public interface AccountPermissionRepository extends BaseRepository<AccountPermissionEntity> {

  List<AccountPermissionEntity> findByAccountId(UUID accountId);

  void deleteByAccountId(UUID accountId);

  @Query("SELECT DISTINCT e.permission FROM AccountPermissionEntity e")
  List<String> findDistinctPermissions();
}

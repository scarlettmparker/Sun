package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.AccountPermissionEntity;
import java.util.List;
import java.util.UUID;

public interface AccountPermissionRepository extends BaseRepository<AccountPermissionEntity> {

  List<AccountPermissionEntity> findByAccountId(UUID accountId);
}

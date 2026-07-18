package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.AccountRoleEntity;
import java.util.List;
import java.util.UUID;

public interface AccountRoleRepository extends BaseRepository<AccountRoleEntity> {

  List<AccountRoleEntity> findByAccountId(UUID accountId);
}

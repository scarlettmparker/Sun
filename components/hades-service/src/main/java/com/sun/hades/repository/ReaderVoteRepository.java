package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderVoteEntity;
import com.sun.hades.model.enums.ReaderVoteTarget;
import java.util.Optional;
import java.util.UUID;

public interface ReaderVoteRepository extends BaseRepository<ReaderVoteEntity> {

  Optional<ReaderVoteEntity> findByAccountIdAndTargetTypeAndTargetId(
      UUID accountId, ReaderVoteTarget targetType, UUID targetId);

  Optional<ReaderVoteEntity> findByAccountIdAndTargetId(UUID accountId, UUID targetId);

  long deleteByAccountIdAndTargetTypeAndTargetId(
      UUID accountId, ReaderVoteTarget targetType, UUID targetId);

  long deleteByTargetTypeAndTargetId(ReaderVoteTarget targetType, UUID targetId);
}

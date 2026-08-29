package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderVoteEntity;
import com.sun.hades.model.enums.ReaderVoteTarget;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReaderVoteRepository extends BaseRepository<ReaderVoteEntity> {

  @Query("select v from ReaderVoteEntity v where v.accountId = :accountId "
      + "and v.targetType = :targetType and v.targetId = :targetId")
  Optional<ReaderVoteEntity> findVote(
      @Param("accountId") UUID accountId,
      @Param("targetType") ReaderVoteTarget targetType,
      @Param("targetId") UUID targetId);

  Optional<ReaderVoteEntity> findByAccountIdAndTargetId(UUID accountId, UUID targetId);

  @Query("select v from ReaderVoteEntity v where v.accountId = :accountId "
      + "and v.targetType = :targetType and v.targetId in :targetIds")
  List<ReaderVoteEntity> findVotes(
      @Param("accountId") UUID accountId,
      @Param("targetType") ReaderVoteTarget targetType,
      @Param("targetIds") Collection<UUID> targetIds);

  @Query("delete from ReaderVoteEntity v where v.accountId = :accountId "
      + "and v.targetType = :targetType and v.targetId = :targetId")
  long deleteVote(
      @Param("accountId") UUID accountId,
      @Param("targetType") ReaderVoteTarget targetType,
      @Param("targetId") UUID targetId);

  long deleteByTargetTypeAndTargetId(ReaderVoteTarget targetType, UUID targetId);
}

package com.sun.icarus.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.icarus.model.ForumVoteEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ForumVoteRepository extends BaseRepository<ForumVoteEntity> {

  Optional<ForumVoteEntity> findByAccountIdAndPostId(UUID accountId, UUID postId);

  List<ForumVoteEntity> findByAccountIdAndPostIdIn(UUID accountId, Collection<UUID> postIds);

  long deleteByAccountIdAndPostId(UUID accountId, UUID postId);

  long deleteByPostId(UUID postId);
}

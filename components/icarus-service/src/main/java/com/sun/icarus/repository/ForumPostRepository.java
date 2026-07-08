package com.sun.icarus.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.icarus.model.ForumPostEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ForumPostRepository extends BaseRepository<ForumPostEntity> {

  Page<ForumPostEntity> findByThreadId(UUID threadId, Pageable pageable);
}

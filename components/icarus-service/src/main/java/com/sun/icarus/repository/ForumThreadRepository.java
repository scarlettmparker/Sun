package com.sun.icarus.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.icarus.model.ForumThreadEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface ForumThreadRepository extends BaseRepository<ForumThreadEntity> {

  @Query(value = "SELECT * FROM icarus_forum_threads WHERE EXISTS "
      + "(SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = ANY(?1))",
      nativeQuery = true)
  List<ForumThreadEntity> findByRemoteObjectsIn(String[] ids);
}

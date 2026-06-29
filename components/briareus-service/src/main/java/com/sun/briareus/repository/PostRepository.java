package com.sun.briareus.repository;

import com.sun.briareus.model.PostEntity;
import com.sun.base.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends BaseRepository<PostEntity> {
  // Domain-specific query methods can be added here

  @Query(value = "SELECT * FROM posts WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = ANY(?1))", nativeQuery = true)
  List<PostEntity> findByRemoteObjectsIn(String[] ids);
}

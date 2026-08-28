package com.sun.briareus.repository;

import com.sun.briareus.model.PostEntity;
import com.sun.base.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepository extends BaseRepository<PostEntity>, JpaSpecificationExecutor<PostEntity> {

  @Query(value = "SELECT * FROM briareus_posts WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = ANY(?1))", nativeQuery = true)
  List<PostEntity> findByRemoteObjectsIn(String[] ids);

  List<PostEntity> findByParentId(UUID parentId);

  Page<PostEntity> findByParentId(UUID parentId, Pageable pageable);
}

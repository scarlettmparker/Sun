package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.TextVersionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TextVersionRepository extends BaseRepository<TextVersionEntity> {

  List<TextVersionEntity> findByTextIdOrderByVersionDesc(UUID textId);

  @Query("SELECT COALESCE(MAX(v.version), 0) FROM TextVersionEntity v WHERE v.textId = :textId")
  int findMaxVersionByTextId(@Param("textId") UUID textId);

  long countByTextId(UUID textId);
}

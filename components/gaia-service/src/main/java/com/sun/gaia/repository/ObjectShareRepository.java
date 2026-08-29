package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.ObjectShareEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObjectShareRepository extends BaseRepository<ObjectShareEntity> {

  List<ObjectShareEntity> findByObjectTypeAndObjectId(String objectType, UUID objectId);

  List<ObjectShareEntity> findByObjectTypeAndObjectIdIn(String objectType, List<UUID> objectIds);

  List<ObjectShareEntity> findBySubjectTypeAndSubjectId(String subjectType, UUID subjectId);

  @Query("select case when count(s)>0 then true else false end from ObjectShareEntity s "
      + "where s.objectType = :objectType and s.objectId = :objectId "
      + "and s.subjectId = :viewerId and s.subjectType = 'user' and s.relation = 'VIEWER'")
  boolean isVisibleToViewer(
      @Param("objectType") String objectType,
      @Param("objectId") UUID objectId,
      @Param("viewerId") UUID viewerId);
}

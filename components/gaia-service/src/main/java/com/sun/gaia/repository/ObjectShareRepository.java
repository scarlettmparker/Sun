package com.sun.gaia.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.gaia.model.ObjectShareEntity;
import java.util.List;
import java.util.UUID;

public interface ObjectShareRepository extends BaseRepository<ObjectShareEntity> {

  List<ObjectShareEntity> findByObjectTypeAndObjectId(String objectType, UUID objectId);

  List<ObjectShareEntity> findBySubjectTypeAndSubjectId(String subjectType, UUID subjectId);

  boolean existsByObjectTypeAndObjectIdAndSubjectTypeAndSubjectId(
      String objectType, UUID objectId, String subjectType, UUID subjectId);

  boolean existsByObjectTypeAndObjectIdAndSubjectId(
      String objectType, UUID objectId, UUID subjectId);
}

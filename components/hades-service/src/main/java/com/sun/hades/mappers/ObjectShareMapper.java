package com.sun.hades.mappers;

import com.sun.gaia.model.ObjectShareEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Maps share data to an entity.
 */
@Component
public class ObjectShareMapper {

  /**
   * Creates a share entity.
   *
   * @param objectType the object type
   * @param objectId the object id
   * @param subjectType the subject type
   * @param subjectId the subject id
   * @param relation the relation
   * @return the entity
   */
  public ObjectShareEntity toEntity(
      String objectType, UUID objectId, String subjectType, UUID subjectId, String relation) {
    ObjectShareEntity share = new ObjectShareEntity();
    share.setObjectType(objectType);
    share.setObjectId(objectId);
    share.setSubjectType(subjectType);
    share.setSubjectId(subjectId);
    share.setRelation(relation);
    return share;
  }
}

package com.sun.gaia.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * A generic share granting a subject access to an ownable object.
 */
@Entity
@Table(
    name = "gaia_object_shares",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_object_shares",
            columnNames = {"object_type", "object_id", "subject_type", "subject_id"}))
public class ObjectShareEntity extends BaseEntity {

  @Column(name = "object_type", nullable = false)
  private String objectType;

  @Column(name = "object_id", nullable = false)
  private UUID objectId;

  @Column(name = "subject_type", nullable = false)
  private String subjectType;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "relation", nullable = false)
  private String relation;

  public String getObjectType() {
    return objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

  public UUID getObjectId() {
    return objectId;
  }

  public void setObjectId(UUID objectId) {
    this.objectId = objectId;
  }

  public String getSubjectType() {
    return subjectType;
  }

  public void setSubjectType(String subjectType) {
    this.subjectType = subjectType;
  }

  public UUID getSubjectId() {
    return subjectId;
  }

  public void setSubjectId(UUID subjectId) {
    this.subjectId = subjectId;
  }

  public String getRelation() {
    return relation;
  }

  public void setRelation(String relation) {
    this.relation = relation;
  }
}

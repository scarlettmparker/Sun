package com.sun.echo.model;

import com.sun.base.model.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Type;
import java.util.List;
import java.util.UUID;

/**
 * Shared shape for checklist "detail" objects (entry/template/item).
 * A detail is a 1:1 sidecar carrying a free-text description and a list of
 * cross-component remote-object references (blog posts, gallery items, image
 * key-details, ...).
 *
 * Concrete subclasses only declare their table; {@code ownerId} (the owning
 * entry/template/item id) and the description/remoteObject fields are inherited
 * here and map to each subclass's own table. The {@code remote_object} column
 * name is fixed explicitly so native queries match Hibernate's physical column.
 */
@MappedSuperclass
public abstract class AbstractDetailEntity extends BaseEntity {

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(columnDefinition = "text")
  private String description;

  @Type(JsonBinaryType.class)
  @Column(name = "remote_object", columnDefinition = "jsonb")
  private List<String> remoteObject;

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getRemoteObject() {
    return remoteObject;
  }

  public void setRemoteObject(List<String> remoteObject) {
    this.remoteObject = remoteObject;
  }
}

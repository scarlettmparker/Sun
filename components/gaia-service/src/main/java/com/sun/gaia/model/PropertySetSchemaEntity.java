package com.sun.gaia.model;

import com.sun.base.model.BaseEntity;
import com.sun.gaia.model.enums.EntryStatus;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Map;
import org.hibernate.annotations.Type;

/** Defines the allowed shape of entries within a named property set. */
@Entity
@Table(name = "gaia_property_set_schemas")
public class PropertySetSchemaEntity extends BaseEntity {

  @Column(name = "owner_key")
  private String ownerKey;

  @Column(name = "name", nullable = false)
  private String name;

  @Type(JsonBinaryType.class)
  @Column(name = "properties", columnDefinition = "jsonb")
  private Map<String, Object> properties;

  @Column(name = "configurable", nullable = false)
  private boolean configurable = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private EntryStatus status = EntryStatus.ACTIVE;

  public String getOwnerKey() {
    return ownerKey;
  }

  public void setOwnerKey(String ownerKey) {
    this.ownerKey = ownerKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  public boolean isConfigurable() {
    return configurable;
  }

  public void setConfigurable(boolean configurable) {
    this.configurable = configurable;
  }

  public EntryStatus getStatus() {
    return status;
  }

  public void setStatus(EntryStatus status) {
    this.status = status;
  }
}

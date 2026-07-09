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

/** A single named entry of values within a property set. */
@Entity
@Table(name = "gaia_property_set_entries")
public class PropertySetEntryEntity extends BaseEntity {

  @Column(name = "owner_key", nullable = false)
  private String ownerKey;

  @Column(name = "property_set", nullable = false)
  private String propertySet;

  @Column(name = "entry_name", nullable = false)
  private String entryName;

  @Type(JsonBinaryType.class)
  @Column(name = "values", columnDefinition = "jsonb")
  private Map<String, Object> values;

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

  public String getPropertySet() {
    return propertySet;
  }

  public void setPropertySet(String propertySet) {
    this.propertySet = propertySet;
  }

  public String getEntryName() {
    return entryName;
  }

  public void setEntryName(String entryName) {
    this.entryName = entryName;
  }

  public Map<String, Object> getValues() {
    return values;
  }

  public void setValues(Map<String, Object> values) {
    this.values = values;
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

package com.sun.gaia.model;

import com.sun.base.model.BaseEntity;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.Type;

/** A declarative desired-state document reconciled into live data on a schedule. */
@Entity
@Table(name = "gaia_configurations")
public class ConfigurationEntity extends BaseEntity {

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Type(JsonBinaryType.class)
  @Column(name = "content", columnDefinition = "jsonb")
  private Map<String, Object> content;

  @Column(name = "last_applied_at")
  private LocalDateTime lastAppliedAt;

  @Column(name = "last_apply_error")
  private String lastApplyError;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Map<String, Object> getContent() {
    return content;
  }

  public void setContent(Map<String, Object> content) {
    this.content = content;
  }

  public LocalDateTime getLastAppliedAt() {
    return lastAppliedAt;
  }

  public void setLastAppliedAt(LocalDateTime lastAppliedAt) {
    this.lastAppliedAt = lastAppliedAt;
  }

  public String getLastApplyError() {
    return lastApplyError;
  }

  public void setLastApplyError(String lastApplyError) {
    this.lastApplyError = lastApplyError;
  }
}

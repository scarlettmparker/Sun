package com.sun.hades.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The website a reader text originated from.
 */
@Entity
@Table(name = "hades_reader_sources")
public class ReaderSourceEntity extends BaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "url", nullable = false, unique = true)
  private String url;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}

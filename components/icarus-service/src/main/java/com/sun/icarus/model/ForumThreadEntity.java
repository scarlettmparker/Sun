package com.sun.icarus.model;

import com.sun.base.model.BaseEntity;
import com.sun.icarus.model.enums.ThreadStatus;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.Type;

/**
 * A discussion thread attached to a remote object.
 */
@Entity
@Table(name = "icarus_forum_threads")
public class ForumThreadEntity extends BaseEntity {

  @Column(name = "title", nullable = false)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ThreadStatus status = ThreadStatus.ACTIVE;

  @Type(JsonBinaryType.class)
  @Column(name = "remote_object", columnDefinition = "jsonb")
  private List<String> remoteObject;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public ThreadStatus getStatus() {
    return status;
  }

  public void setStatus(ThreadStatus status) {
    this.status = status;
  }

  public List<String> getRemoteObject() {
    return remoteObject;
  }

  public void setRemoteObject(List<String> remoteObject) {
    this.remoteObject = remoteObject;
  }
}

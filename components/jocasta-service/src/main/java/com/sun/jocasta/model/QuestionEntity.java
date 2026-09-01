package com.sun.jocasta.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Immutable question, no updatedAt.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jocasta_questions")
public class QuestionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "stem", columnDefinition = "TEXT", nullable = false)
  private String stem;

  @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
  private String answer;

  @Column(name = "explanation", columnDefinition = "TEXT")
  private String explanation;

  @Type(JsonBinaryType.class)
  @Column(name = "remote_object", columnDefinition = "jsonb", nullable = false)
  private List<String> remoteObject;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp(6) default current_timestamp")
  private LocalDateTime createdAt;

  @CreatedBy
  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getStem() {
    return stem;
  }

  public void setStem(String stem) {
    this.stem = stem;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public List<String> getRemoteObject() {
    return remoteObject;
  }

  public void setRemoteObject(List<String> remoteObject) {
    this.remoteObject = remoteObject;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UUID createdBy) {
    this.createdBy = createdBy;
  }
}

package com.sun.jocasta.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable answer attempt.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jocasta_answers")
public class AnswerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Column(name = "my_answer", columnDefinition = "TEXT", nullable = false)
  private String myAnswer;

  @Column(name = "correct", nullable = false)
  private boolean correct;

  @Column(name = "correct_answer", columnDefinition = "TEXT", nullable = false)
  private String correctAnswer;

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

  public UUID getQuestionId() {
    return questionId;
  }

  public void setQuestionId(UUID questionId) {
    this.questionId = questionId;
  }

  public String getMyAnswer() {
    return myAnswer;
  }

  public void setMyAnswer(String myAnswer) {
    this.myAnswer = myAnswer;
  }

  public boolean isCorrect() {
    return correct;
  }

  public void setCorrect(boolean correct) {
    this.correct = correct;
  }

  public String getCorrectAnswer() {
    return correctAnswer;
  }

  public void setCorrectAnswer(String correctAnswer) {
    this.correctAnswer = correctAnswer;
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

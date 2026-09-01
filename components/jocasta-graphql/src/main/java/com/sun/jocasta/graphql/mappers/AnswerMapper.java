package com.sun.jocasta.graphql.mappers;

import com.sun.jocasta.codegen.types.Answer;
import com.sun.jocasta.model.AnswerEntity;
import org.springframework.stereotype.Component;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class AnswerMapper {

  /**
   * Maps answer entity to GraphQL.
   *
   * @param e - entity
   * @return graphql answer
   */
  public Answer map(AnswerEntity e) {
    return Answer.newBuilder()
        .id(e.getId().toString())
        .questionId(e.getQuestionId().toString())
        .myAnswer(e.getMyAnswer())
        .correct(e.isCorrect())
        .correctAnswer(e.getCorrectAnswer())
        .createdAt(e.getCreatedAt() == null ? null : e.getCreatedAt().atOffset(ZoneOffset.UTC))
        .build();
  }

  /**
   * Maps answer submission to entity.
   *
   * @param questionId - question id
   * @param myAnswer - learner answer
   * @param correct - whether correct
   * @param correctAnswer - correct text
   * @return entity
   */
  public AnswerEntity mapInput(String questionId, String myAnswer, Boolean correct, String correctAnswer) {
    AnswerEntity e = new AnswerEntity();
    e.setQuestionId(UUID.fromString(questionId));
    e.setMyAnswer(myAnswer == null ? "" : myAnswer);
    e.setCorrect(correct != null && correct);
    e.setCorrectAnswer(correctAnswer == null ? "" : correctAnswer);
    return e;
  }
}

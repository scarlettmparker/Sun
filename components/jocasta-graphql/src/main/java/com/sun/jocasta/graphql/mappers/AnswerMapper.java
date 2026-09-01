package com.sun.jocasta.graphql.mappers;

import com.sun.jocasta.codegen.types.Answer;
import com.sun.jocasta.codegen.types.AnswerInput;
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
   * @param input - answer input
   * @return entity
   */
  public AnswerEntity mapInput(String questionId, AnswerInput input) {
    AnswerEntity e = new AnswerEntity();
    e.setQuestionId(UUID.fromString(questionId));
    e.setMyAnswer(input.getMyAnswer() == null ? "" : input.getMyAnswer());
    e.setCorrect(input.getCorrect() != null && input.getCorrect());
    e.setCorrectAnswer(input.getCorrectAnswer() == null ? "" : input.getCorrectAnswer());
    return e;
  }
}

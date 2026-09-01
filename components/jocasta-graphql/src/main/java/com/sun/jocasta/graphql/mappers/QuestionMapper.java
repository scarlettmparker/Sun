package com.sun.jocasta.graphql.mappers;

import com.sun.jocasta.codegen.types.Question;
import com.sun.jocasta.codegen.types.QuestionInput;
import com.sun.jocasta.model.QuestionEntity;
import org.springframework.stereotype.Component;
import java.time.ZoneOffset;

@Component
public class QuestionMapper {

  /**
   * Maps entity to GraphQL.
   *
   * @param e - entity
   * @return graphql type
   */
  public Question map(QuestionEntity e) {
    return Question.newBuilder()
        .id(e.getId().toString())
        .stem(e.getStem())
        .answer(e.getAnswer())
        .explanation(e.getExplanation())
        .remoteObject(e.getRemoteObject())
        .createdAt(e.getCreatedAt() == null ? null : e.getCreatedAt().atOffset(ZoneOffset.UTC))
        .build();
  }

  /**
   * Maps input to entity.
   *
   * @param input - graphql input
   * @return entity
   */
  public QuestionEntity mapInput(QuestionInput input) {
    QuestionEntity e = new QuestionEntity();
    e.setStem(input.getStem());
    e.setAnswer(input.getAnswer());
    e.setExplanation(input.getExplanation());
    e.setRemoteObject(input.getRemoteObject());
    return e;
  }
}

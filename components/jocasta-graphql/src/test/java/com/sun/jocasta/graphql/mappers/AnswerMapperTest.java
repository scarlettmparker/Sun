package com.sun.jocasta.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.jocasta.model.AnswerEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnswerMapper")
class AnswerMapperTest {

  private final AnswerMapper mapper = new AnswerMapper();

  @Test
  @DisplayName("map should map all fields")
  void map_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 2, 11, 0);
    UUID id = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();

    AnswerEntity entity = new AnswerEntity();
    entity.setId(id);
    entity.setQuestionId(questionId);
    entity.setMyAnswer("my answer");
    entity.setCorrect(true);
    entity.setCorrectAnswer("correct");
    entity.setCreatedAt(createdAt);
    entity.setCreatedBy(UUID.randomUUID());

    var result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getQuestionId()).isEqualTo(questionId.toString());
    assertThat(result.getMyAnswer()).isEqualTo("my answer");
    assertThat(result.getCorrect()).isTrue();
    assertThat(result.getCorrectAnswer()).isEqualTo("correct");
    assertThat(result.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("mapInput should map all fields")
  void mapInput_shouldMapAllFields() {
    UUID questionId = UUID.randomUUID();

    AnswerEntity result = mapper.mapInput(questionId.toString(), "my", true, "corr");

    assertThat(result.getQuestionId()).isEqualTo(questionId);
    assertThat(result.getMyAnswer()).isEqualTo("my");
    assertThat(result.isCorrect()).isTrue();
    assertThat(result.getCorrectAnswer()).isEqualTo("corr");
  }

  @Test
  @DisplayName("mapInput should handle nulls")
  void mapInput_shouldHandleNulls() {
    UUID questionId = UUID.randomUUID();

    AnswerEntity result = mapper.mapInput(questionId.toString(), null, null, null);

    assertThat(result.getMyAnswer()).isEqualTo("");
    assertThat(result.isCorrect()).isFalse();
    assertThat(result.getCorrectAnswer()).isEqualTo("");
  }
}

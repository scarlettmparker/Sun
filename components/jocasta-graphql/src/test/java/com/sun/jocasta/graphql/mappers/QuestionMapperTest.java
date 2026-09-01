package com.sun.jocasta.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.jocasta.codegen.types.QuestionInput;
import com.sun.jocasta.model.QuestionEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QuestionMapper")
class QuestionMapperTest {

  private final QuestionMapper mapper = new QuestionMapper();

  @Test
  @DisplayName("map should map all fields")
  void map_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
    UUID id = UUID.randomUUID();

    QuestionEntity entity = new QuestionEntity();
    entity.setId(id);
    entity.setStem("What is ____ [concept]?");
    entity.setAnswer("categorical imperative");
    entity.setExplanation("categorical imperative is moral law");
    entity.setRemoteObject(List.of("briareus:post:123"));
    entity.setCreatedAt(createdAt);
    entity.setCreatedBy(UUID.randomUUID());

    var result = mapper.map(entity);

    assertThat(result.getId()).isEqualTo(id.toString());
    assertThat(result.getStem()).isEqualTo("What is ____ [concept]?");
    assertThat(result.getAnswer()).isEqualTo("categorical imperative");
    assertThat(result.getExplanation()).isEqualTo("categorical imperative is moral law");
    assertThat(result.getRemoteObject()).containsExactly("briareus:post:123");
    assertThat(result.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("mapInput should map all fields")
  void mapInput_shouldMapAllFields() {
    QuestionInput input = QuestionInput.newBuilder()
        .stem("Fill ____ [year]")
        .answer("1785")
        .explanation("published in 1785")
        .remoteObject(List.of("briareus:post:456"))
        .build();

    QuestionEntity result = mapper.mapInput(input);

    assertThat(result.getStem()).isEqualTo("Fill ____ [year]");
    assertThat(result.getAnswer()).isEqualTo("1785");
    assertThat(result.getExplanation()).isEqualTo("published in 1785");
    assertThat(result.getRemoteObject()).containsExactly("briareus:post:456");
  }
}

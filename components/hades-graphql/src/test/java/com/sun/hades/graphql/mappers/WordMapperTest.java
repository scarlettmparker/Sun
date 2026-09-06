package com.sun.hades.graphql.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.graphql.domain.WordDefinition;
import com.sun.hades.graphql.domain.WordDefinitionEntry;
import com.sun.hades.graphql.domain.WordDefinitionTranslation;
import java.util.List;
import org.junit.jupiter.api.Test;

class WordMapperTest {

  private final WordMapper mapper = new WordMapper();

  @Test
  void map_shouldMapAllFields() {
    WordDefinition domain = new WordDefinition(
        "hello",
        "hello",
        List.of(new WordDefinitionEntry(
            "en:rh:hello:0",
            "hello",
            "interj.",
            null,
            List.of(new WordDefinitionTranslation("greeting used on meeting", null, List.of())),
            List.of("Hello!"),
            null)),
        List.of(),
        List.of(new WordDefinition.RelatedWord("hi", "https://www.wordreference.com/definition/hi")),
        "https://www.wordreference.com/definition/hello");

    var result = mapper.map(domain);

    assertThat(result.getId()).isEqualTo("hello");
    assertThat(result.getTerm()).isEqualTo("hello");
    assertThat(result.getEntries()).hasSize(1);
    assertThat(result.getEntries().get(0).getId()).isEqualTo("en:rh:hello:0");
    assertThat(result.getEntries().get(0).getTranslations().get(0).getTerm())
        .isEqualTo("greeting used on meeting");
    assertThat(result.getEntries().get(0).getExamples()).containsExactly("Hello!");
    assertThat(result.getCompounds()).isEmpty();
    assertThat(result.getRelatedWords()).hasSize(1);
    assertThat(result.getRelatedWords().get(0).getTerm()).isEqualTo("hi");
    assertThat(result.getSourceUrl()).isEqualTo("https://www.wordreference.com/definition/hello");
  }

  @Test
  void map_shouldReturnNullWhenNull() {
    assertThat(mapper.map(null)).isNull();
  }
}

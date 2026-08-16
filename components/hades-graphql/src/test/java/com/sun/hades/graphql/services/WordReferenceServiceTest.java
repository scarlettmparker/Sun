package com.sun.hades.graphql.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.hades.codegen.types.Word;
import com.sun.hades.codegen.types.WordScope;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class WordReferenceServiceTest {

  private final WordReferenceService service = new WordReferenceService();

  @Test
  void defineWord_defaultsToTheFirstTwoEntries() throws Exception {
    Word word = service.parseWord(fixture("wordreference/geia.html"), "γεια", List.of());

    assertThat(word.getTerm()).isEqualTo("γεια");
    assertThat(word.getEntries()).hasSize(2);
    assertThat(word.getEntries().get(0).getWordType()).isEqualTo("επιφ");
    assertThat(word.getEntries().get(0).getTranslations())
        .extracting(com.sun.hades.codegen.types.WordTranslation::getTerm)
        .containsExactly("hello", "hi, hey", "goodbye", "bye, bye-bye");
    assertThat(word.getEntries().get(0).getNote()).isNull();
    assertThat(word.getEntries().get(1).getWordType()).isEqualTo("ουσ ουδ άκλ");
    assertThat(word.getEntries().get(0).getExamples()).isEmpty();
    assertThat(word.getCompounds()).isEmpty();
    assertThat(word.getRelatedWords()).isEmpty();
  }

  @Test
  void defineWord_withEveryScopePopulatesEachSection() throws Exception {
    Word word = service.parseWord(
        fixture("wordreference/geia.html"),
        "γεια",
        List.of(
            WordScope.ALL_TRANSLATIONS,
            WordScope.EXAMPLES,
            WordScope.COMPOUNDS,
            WordScope.RELATED_WORDS));

    assertThat(word.getEntries()).hasSize(2);
    assertThat(word.getEntries().get(0).getExamples())
        .containsExactly("Γεια Ελένη, τι κάνεις;");
    assertThat(word.getEntries().get(0).getTranslations().get(0).getUsageNotes())
        .containsExactly("when coming");
    assertThat(word.getCompounds()).isNotEmpty();
    assertThat(word.getCompounds().get(0).getTerm()).isNotBlank();
    assertThat(word.getRelatedWords()).isNotEmpty();
    assertThat(word.getRelatedWords().get(0).getTerm()).isNotBlank();
    assertThat(word.getRelatedWords().get(0).getSourceUrl())
        .startsWith("https://www.wordreference.com/gren/");
  }

  @Test
  void defineWord_returnsNullWhenThePageHasNoDictionary() throws Exception {
    assertThat(service.parseWord(fixture("wordreference/missing.html"), "λλλλλλ", List.of()))
        .isNull();
  }

  @Test
  void defineWord_fallsBackToTheReverseTableWhenOnlyItExists() throws Exception {
    Word word = service.parseWord(
        fixture("wordreference/oualia.html"),
        "ουαλία",
        List.of(WordScope.ALL_TRANSLATIONS));

    assertThat(word.getEntries()).isNotEmpty();
    assertThat(word.getEntries().get(0).getTerm()).isEqualTo("Wales");
    assertThat(word.getEntries().get(0).getTranslations())
        .extracting(com.sun.hades.codegen.types.WordTranslation::getTerm)
        .contains("Ουαλία");
  }

  private static String fixture(String path) throws Exception {
    return new String(
        new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }
}

package com.sun.hades.graphql.mappers;

import com.sun.hades.codegen.types.Word;
import com.sun.hades.codegen.types.WordEntry;
import com.sun.hades.codegen.types.WordTranslation;
import com.sun.hades.graphql.domain.WordDefinition;
import com.sun.hades.graphql.domain.WordDefinitionEntry;
import com.sun.hades.graphql.domain.WordDefinitionTranslation;
import org.springframework.stereotype.Component;

/**
 * Maps word definition domain types to GraphQL types.
 */
@Component
public class WordMapper {

  /**
   * Maps a word definition to GraphQL.
   *
   * @param domain the parsed word definition
   * @return the GraphQL word or null
   */
  public Word map(WordDefinition domain) {
    if (domain == null) {
      return null;
    }
    return Word.newBuilder()
        .id(domain.id())
        .term(domain.term())
        .entries(domain.entries().stream().map(this::mapEntry).toList())
        .compounds(domain.compounds().stream().map(this::mapEntry).toList())
        .relatedWords(domain.relatedWords().stream()
            .map(related -> Word.newBuilder()
                .id(related.term())
                .term(related.term())
                .entries(java.util.List.of())
                .compounds(java.util.List.of())
                .relatedWords(java.util.List.of())
                .sourceUrl(related.sourceUrl())
                .build())
            .toList())
        .sourceUrl(domain.sourceUrl())
        .build();
  }

  /**
   * Maps an entry to GraphQL.
   *
   * @param domain the entry
   * @return the GraphQL entry
   */
  public WordEntry mapEntry(WordDefinitionEntry domain) {
    return WordEntry.newBuilder()
        .id(domain.id())
        .term(domain.term())
        .wordType(domain.wordType())
        .sense(domain.sense())
        .translations(domain.translations().stream().map(this::mapTranslation).toList())
        .examples(domain.examples())
        .note(domain.note())
        .build();
  }

  /**
   * Maps a translation to GraphQL.
   *
   * @param domain the translation
   * @return the GraphQL translation
   */
  public WordTranslation mapTranslation(WordDefinitionTranslation domain) {
    return WordTranslation.newBuilder()
        .term(domain.term())
        .wordType(domain.wordType())
        .usageNotes(domain.usageNotes())
        .build();
  }
}

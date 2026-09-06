package com.sun.hades.graphql.domain;

import java.util.List;

/**
 * Word definition parsed from WordReference.
 */
public record WordDefinition(
    String id,
    String term,
    List<WordDefinitionEntry> entries,
    List<WordDefinitionEntry> compounds,
    List<RelatedWord> relatedWords,
    String sourceUrl) {

  /**
   * Shallow related word.
   */
  public record RelatedWord(String term, String sourceUrl) {}
}

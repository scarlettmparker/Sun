package com.sun.hades.graphql.domain;

import java.util.List;

/**
 * Single entry within a word definition.
 */
public record WordDefinitionEntry(
    String id,
    String term,
    String wordType,
    String sense,
    List<WordDefinitionTranslation> translations,
    List<String> examples,
    String note) {}

package com.sun.hades.graphql.domain;

import java.util.List;

/**
 * Translation within an entry.
 */
public record WordDefinitionTranslation(String term, String wordType, List<String> usageNotes) {}

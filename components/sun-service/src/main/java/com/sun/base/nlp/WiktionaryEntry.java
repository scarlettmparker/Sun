package com.sun.base.nlp;

import java.util.List;

public record WiktionaryEntry(
    /**
     * Headword.
     */
    String word,
    /**
     * English definitions.
     */
    List<String> definitions,
    /**
     * Source page url.
     */
    String sourceUrl) {
}
